/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.ingest.IngestResult;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngestHeaders;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.URISupport;
import org.jboss.logging.Logger;

/**
 * Translates the extension's configuration model — build-time and runtime properties and
 * {@code @Ingest} builder methods — into one composition route per pipeline over the
 * langchain4j-ingest Kamelets: the {@code langchain4j-ingest-file-source} Kamelet (or any
 * consumer URI), through the {@code tika-extract-text-action} or {@code docling-convert-action}
 * Kamelet when a parser is configured, into the {@code langchain4j-ingest-sink} Kamelet, whose
 * engine is the {@code camel-langchain4j-ingest} component. The topology lives in the Kamelet
 * catalog; what stays here is the Quarkus DX: CDI bean resolution with its actionable messages,
 * the configuration-level validations and the pre-parse raw-size guard.
 */
@ApplicationScoped
public class IngestRoutes extends RouteBuilder {

    /**
     * The built-in register capacity, sized above Camel's 1000-entry default so eviction does
     * not re-ingest large directories during normal operation; in-memory, so lost on restart.
     */
    static final int DEFAULT_REGISTER_CAPACITY = 100_000;

    private static final Logger LOG = Logger.getLogger(IngestRoutes.class);

    @Inject
    IngestBuildTimeConfig buildTimeConfig;

    @Inject
    IngestRunTimeConfig runTimeConfig;

    @Inject
    IngestBuilderPipelines builderPipelines;

    // these injection points also keep an unnamed store or model bean from being removed as
    // unused - nothing else in the application need inject it
    @Inject
    @Any
    Instance<EmbeddingStore<TextSegment>> storeCandidates;

    @Inject
    @Any
    Instance<EmbeddingModel> modelCandidates;

    @Override
    public void configure() {
        // a pipeline may be declared entirely through runtime properties - the documented
        // minimum is a directory and nothing else - so the two config roots are unioned. Keying
        // off the build-time map alone would make that configuration a silent no-op, since
        // SmallRye only materialises a map key for the mapping whose structure a property matches
        Set<String> builderDeclared = builderPipelines.entries().stream()
                .map(IngestBuilderPipelines.Entry::name)
                .collect(Collectors.toSet());
        Set<String> names = new TreeSet<>(buildTimeConfig.pipelines().keySet());
        names.addAll(runTimeConfig.pipelines().keySet());
        names.removeAll(builderDeclared);

        for (String name : names) {
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = buildTimeConfig.pipelines().get(name);
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);

            if (runtime != null && !runtime.enabled()) {
                LOG.infof("Ingestion pipeline '%s' is disabled", name);
                continue;
            }

            // a consumer URI says "consume from this"; its absence says "read that directory"
            String uri = pipeline == null ? null : pipeline.source().uri().orElse(null);
            if (uri != null && runtime != null && runtime.source().directory().isPresent()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' sets both source.uri ('" + uri
                        + "') and source.directory ('" + runtime.source().directory().get() + "'). A pipeline "
                        + "reads one source: keep the URI, or drop it to read the directory.");
            }

            compositionRoute(name, uri, runtime,
                    resolveStore(name, pipeline == null ? null : pipeline.embeddingStore().orElse(null)),
                    resolveModel(name, pipeline == null ? null : pipeline.embeddingModel().orElse(null)),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE : pipeline.maxSegmentSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE : pipeline.maxOverlapSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_EMBEDDING_BATCH_SIZE : pipeline.embeddingBatchSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_DOCUMENT_SIZE : pipeline.maxDocumentSize(),
                    pipeline == null ? null : pipeline.documentSplitter().orElse(null),
                    pipeline == null ? null : pipeline.parser().orElse(null));
        }

        for (IngestBuilderPipelines.Entry entry : builderPipelines.entries()) {
            builderPipeline(entry);
        }
    }

    /** An {@code @Ingest}-declared pipeline: the builder twin of the configuration path. */
    private void builderPipeline(IngestBuilderPipelines.Entry entry) {
        String name = entry.name();
        // configuration can still switch a builder-declared pipeline off, and the check precedes
        // the invocation so a disabled pipeline's method never runs
        IngestRunTimeConfig.PipelineRunTimeConfig external = runTimeConfig.pipelines().get(name);
        if (external != null && !external.enabled()) {
            LOG.infof("Ingestion pipeline '%s' (builder) is disabled", name);
            return;
        }
        // enabled is the one thing configuration may say about a builder pipeline; anything about
        // its source would be quietly overruled by the @Ingest method, so it is an error instead
        // (source.recursive cannot be told apart from its default, so it alone goes undetected -
        // Source.recursive() is its builder twin)
        if (external != null && (external.source().directory().isPresent()
                || external.source().documentId().isPresent()
                || external.source().idempotentRepository().isPresent()
                || external.source().idempotentRepositoryAutoCreate())) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' is declared in Java, so its source "
                    + "comes from the @Ingest method. Remove quarkus.camel.langchain4j.ingest." + name + ".source.* , or "
                    + "declare the pipeline in configuration instead.");
        }

        IngestPipeline definition = builderPipelines.definition(entry);
        String uri = "file".equals(definition.sourceType()) ? null : definition.sourceUri();
        compositionRoute(name, uri, definition.asRunTimeConfig(),
                resolveStore(name, definition.embeddingStoreName().orElse(null)),
                resolveModel(name, definition.embeddingModelName().orElse(null)),
                definition.maxSegmentSize(),
                definition.maxOverlapSize(),
                definition.embeddingBatchSize(),
                definition.maxDocumentSize(),
                definition.documentSplitterName().orElse(null),
                definition.parser().orElse(null));
    }

    /**
     * One composition route for both declaration styles: the file-source Kamelet (or the
     * configured consumer URI), through a parser action Kamelet when {@code parser} is set, into
     * the sink Kamelet. The document id is normalised into the
     * {@code CamelLangChain4jIngestDocumentId} header before any parse; the parser actions
     * capture it into the exchange property the sink's endpoint resolves with property-over-header
     * precedence, so a crafted document cannot forge its own identity through parser-copied
     * metadata headers.
     */
    private void compositionRoute(String name, String uri,
            IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            EmbeddingStore<TextSegment> store, EmbeddingModel model,
            int maxSegmentSize, int maxOverlapSize, int embeddingBatchSize, int maxDocumentSize,
            String documentSplitterName, String parser) {

        String storeRef = bindInstance(name, "store", store);
        String modelRef = bindInstance(name, "model", model);
        String documentId = runtime == null ? null : runtime.source().documentId().orElse(null);
        String repositoryRef = repositoryRef(name, runtime, uri == null);

        Map<String, Object> sink = new LinkedHashMap<>();
        sink.put("pipelineName", name);
        sink.put("maxSegmentSize", String.valueOf(maxSegmentSize));
        sink.put("maxOverlapSize", String.valueOf(maxOverlapSize));
        sink.put("embeddingBatchSize", String.valueOf(embeddingBatchSize));
        if (maxDocumentSize > 0) {
            sink.put("maxDocumentSize", String.valueOf(maxDocumentSize));
        }
        if (documentSplitterName != null) {
            sink.put("documentSplitter", "#bean:" + documentSplitterName);
        }
        sink.put("embeddingStore", "#bean:" + storeRef);
        sink.put("embeddingModel", "#bean:" + modelRef);

        if (uri == null) {
            String directory = required(name, runtime == null ? null : runtime.source().directory().orElse(null),
                    "source.directory");
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("directory", directory);
            source.put("recursive", String.valueOf(runtime.source().recursive()));
            if (parser == null) {
                // text is read as UTF-8; a parser receives the raw bytes instead - the format is
                // its business, and a charset conversion would corrupt a binary document
                source.put("charset", "UTF-8");
            }
            source.put("idempotentRepository", "#bean:" + repositoryRef);

            ProcessorDefinition<?> route = from(kameletUri("langchain4j-ingest-file-source", source))
                    .routeId(routeId(name));
            if (documentId != null) {
                // override the source's file-name default; captured before any further step
                route = route.setHeader(LangChain4jIngestHeaders.DOCUMENT_ID,
                        documentIdExpression(documentId));
            }
            route = parseSteps(route, name, parser, maxDocumentSize, true);
            // the file consumer discards the reply and the source register already keeps the
            // same file version from being ingested twice, so no repository goes to the sink
            emptyOutcomeTail(route.to(kameletUri("langchain4j-ingest-sink", sink)), name, parser);
            LOG.infof("Ingestion pipeline '%s': source=file:%s", name, directory);
        } else {
            ProcessorDefinition<?> route = from(uri).routeId(routeId(name));
            String documentIdHeader;
            if (documentId != null && isSimpleExpression(documentId)) {
                // evaluated against the exchange the consumer delivered, before any parse
                route = route.setHeader(LangChain4jIngestHeaders.DOCUMENT_ID, documentIdExpression(documentId));
                documentIdHeader = LangChain4jIngestHeaders.DOCUMENT_ID;
            } else {
                // a plain header name goes to the actions and the endpoint as-is; unset keeps
                // this extension's released default (CamelIngestDocumentId) - the compatibility
                // burden stays downstream, where it was created
                documentIdHeader = documentId != null ? documentId : IngestHeaders.DOCUMENT_ID;
            }
            sink.put("documentIdHeader", documentIdHeader);
            route = parseSteps(route, name, parser, maxDocumentSize, false, documentIdHeader);
            if (repositoryRef != null) {
                // deduplication by document id happens inside the sink's producer: a duplicate
                // is answered SKIPPED, a blank delivery releases its claim
                sink.put("idempotentRepository", "#bean:" + repositoryRef);
            }
            route.to(kameletUri("langchain4j-ingest-sink", sink));
            LOG.infof("Ingestion pipeline '%s': source=%s", name, URISupport.sanitizeUri(uri));
        }
    }

    /**
     * The optional parse stage: the raw-size guard, then the parser action Kamelet, which
     * captures the document id into the exchange property before the parse. The route is
     * returned unchanged when the pipeline has no parser.
     */
    private ProcessorDefinition<?> parseSteps(ProcessorDefinition<?> route, String name, String parser,
            int maxDocumentSize, boolean directory) {
        // the file-source Kamelet normalised the id into the upstream header already
        return parseSteps(route, name, parser, maxDocumentSize, directory, LangChain4jIngestHeaders.DOCUMENT_ID);
    }

    private ProcessorDefinition<?> parseSteps(ProcessorDefinition<?> route, String name, String parser,
            int maxDocumentSize, boolean directory, String documentIdHeader) {
        if (parser == null) {
            return route;
        }
        if (maxDocumentSize > 0) {
            // the endpoint's own cap counts extracted characters, which protects the splitter and
            // the model but not the parse: this guard rejects the raw payload first, before tika
            // or docling materialize it
            route = route.process(rawSizeGuard(name, maxDocumentSize, directory, documentIdHeader));
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("documentIdHeader", documentIdHeader);
        return switch (parser) {
        case "tika" -> route.to(kameletUri("tika-extract-text-action", action));
        case "docling" -> route.to(kameletUri("docling-convert-action", action));
        default -> throw new IllegalStateException("Unknown parser " + parser);
        };
    }

    private static Processor rawSizeGuard(String name, int maxDocumentSize, boolean trustDeclaredLength,
            String documentIdHeader) {
        // the declared-length header spares even the read, but only the directory pipeline's own
        // file consumer is trusted to have set it: on a consumer pipeline every header may be
        // attacker-supplied along with the payload, so its body is always measured - a forged
        // CamelFileLength must not talk an oversized payload past the guard and into the parser
        return exchange -> {
            Long declared = trustDeclaredLength
                    ? exchange.getMessage().getHeader(Exchange.FILE_LENGTH, Long.class)
                    : null;
            long size;
            byte[] bounded = null;
            if (declared != null) {
                size = declared;
            } else if (exchange.getMessage().getBody() instanceof InputStream stream) {
                // bounded read: an attacker-sized stream is rejected after maxDocumentSize + 1
                // bytes instead of being materialized whole in the heap just to be measured;
                // an accepted stream is consumed here, so the bytes replace it as the body
                int limit = maxDocumentSize == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxDocumentSize + 1;
                bounded = stream.readNBytes(limit);
                size = bounded.length;
            } else {
                // a null body carries no bytes to guard; it flows on and becomes the EMPTY outcome
                byte[] body = exchange.getMessage().getBody(byte[].class);
                size = body == null ? 0 : body.length;
            }
            if (size > maxDocumentSize) {
                throw new IllegalArgumentException(
                        "Ingestion pipeline '" + name + "': document '"
                                + exchange.getMessage().getHeader(documentIdHeader, String.class)
                                + "' exceeds maxDocumentSize (" + size + " > " + maxDocumentSize + " bytes)");
            }
            if (bounded != null) {
                exchange.getMessage().setBody(bounded);
            }
        };
    }

    /**
     * A directory pipeline's file consumer discards the reply, so an EMPTY outcome would leave
     * no trace at all: with a parser it is warned about — a parse to nothing typically means a
     * missing Tika parser module or an image-only document, and the file's register key is
     * committed, so it is not retried until the file changes — without one it is debug-logged.
     */
    private void emptyOutcomeTail(ProcessorDefinition<?> tail, String name, String parser) {
        tail.process(exchange -> {
            IngestResult result = exchange.getMessage().getBody(IngestResult.class);
            if (result == null || result.outcome() != IngestResult.Outcome.EMPTY) {
                return;
            }
            if (parser != null) {
                LOG.warnf("Ingestion pipeline '%s': document '%s' parsed to no text and was skipped; its key is"
                        + " committed, so it is not retried until the file changes (missing parser module?"
                        + " image-only document?)", name, result.documentId());
            } else {
                LOG.debugf("Ingestion pipeline '%s': document '%s' contained no text, nothing was written",
                        name, result.documentId());
            }
        });
    }

    /**
     * Resolves the pipeline's duplicate register to a registry reference: a named bean (existence
     * checked up front, with the configuration-level message), an auto-created in-memory register
     * bound under the configured name, or - for a directory pipeline naming none - a generated
     * built-in one, sized above the file endpoint's default so eviction does not re-ingest large
     * directories.
     */
    private String repositoryRef(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime, boolean directory) {
        String repositoryName = runtime == null ? null : runtime.source().idempotentRepository().orElse(null);
        boolean autoCreate = runtime != null && runtime.source().idempotentRepositoryAutoCreate();
        if (autoCreate && repositoryName == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name
                    + "' sets source.idempotent-repository-auto-create but no "
                    + "source.idempotent-repository name to create the register under.");
        }
        if (repositoryName != null) {
            IdempotentRepository repository = getContext().getRegistry().lookupByNameAndType(repositoryName,
                    IdempotentRepository.class);
            if (repository == null) {
                if (!autoCreate) {
                    throw new IllegalStateException("Ingestion pipeline '" + name
                            + "' references idempotent repository '" + repositoryName + "' but no such bean exists");
                }
                getContext().getRegistry().bind(repositoryName,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            } else {
                // a CDI-produced repository does not pass through the registry's bind hook, so a
                // CamelContextAware implementation would otherwise run contextless
                CamelContextAware.trySetCamelContext(repository, getContext());
            }
            return repositoryName;
        }
        if (directory) {
            String ref = "langchain4j-ingest-" + name + "-register";
            if (getContext().getRegistry().lookupByNameAndType(ref, IdempotentRepository.class) == null) {
                getContext().getRegistry().bind(ref,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            }
            return ref;
        }
        return null;
    }

    /** Binds a CDI-resolved instance to the registry, so the Kamelet can reference it. */
    private String bindInstance(String name, String what, Object instance) {
        String ref = "langchain4j-ingest-" + name + "-" + what;
        getContext().getRegistry().bind(ref, instance);
        return ref;
    }

    /**
     * A bare header name is read as a header directly rather than parsed: a dotted header name
     * would send the simple parser into OGNL. The expression is initialised here, at route build
     * time — left to reify lazily it would race on the first concurrent exchanges.
     */
    private Expression documentIdExpression(String configured) {
        Expression expression = isSimpleExpression(configured)
                ? ExpressionBuilder.simpleExpression(configured)
                : ExpressionBuilder.headerExpression(configured);
        expression.init(getContext());
        return expression;
    }

    private static boolean isSimpleExpression(String value) {
        return value.contains("${") || value.contains("$simple{");
    }

    /**
     * Built through {@code createQueryString} rather than concatenated, so no Kamelet property can be injected. The
     * {@code #bean:} prefix of registry references is restored afterwards: percent-encoded it would survive the
     * template substitution literally and reach the inner endpoint as text instead of a bean lookup.
     */
    private static String kameletUri(String kamelet, Map<String, Object> properties) {
        return "kamelet:" + kamelet + "?" + URISupport.createQueryString(properties).replace("%23bean%3A", "#bean:");
    }

    private static String routeId(String name) {
        return "langchain4j-ingest-" + name;
    }

    private static String required(String name, String value, String property) {
        if (value == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' has no " + property
                    + ". Set quarkus.camel.langchain4j.ingest." + name + "." + property);
        }
        return value;
    }

    private EmbeddingStore<TextSegment> resolveStore(String name, String configured) {
        return resolve(name, storeCandidates, configured, "embedding store", "embedding-store");
    }

    private EmbeddingModel resolveModel(String name, String configured) {
        return resolve(name, modelCandidates, configured, "embedding model", "embedding-model");
    }

    /**
     * CDI is the one mechanism for both lookups: the named path selects on the qualifier, the
     * unnamed path counts the candidates — through handles, so beans are not instantiated merely
     * to be counted. Picking one silently would bind a pipeline to whichever bean happened to be
     * discovered first. A raw-typed registry search cannot serve here: it never matches a bean
     * typed {@code EmbeddingStore<TextSegment>}.
     */
    private <T> T resolve(String name, Instance<T> candidates, String configured, String what, String property) {
        if (configured != null) {
            Instance<T> named = candidates.select(NamedLiteral.of(configured));
            if (named.isUnsatisfied()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' references " + what + " '"
                        + configured + "' but no such bean exists");
            }
            return named.get();
        }
        List<Instance.Handle<T>> handles = StreamSupport.stream(candidates.handles().spliterator(), false)
                .collect(Collectors.toList());
        if (handles.isEmpty()) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' needs an " + what
                    + ", but no bean of that type exists. Define one, for example with a @Produces method.");
        }
        if (handles.size() > 1) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' found " + handles.size() + " "
                    + what + " beans. Name the one to use with quarkus.camel.langchain4j.ingest." + name + "."
                    + property);
        }
        return handles.get(0).get();
    }
}
