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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.AdoptPlan;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.IngestResult;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.IngestService;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.SyncPassRunner;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.jdbc.JdbcIngestionTracker;
import org.jboss.logging.Logger;

/**
 * Generates one Camel route per configured ingestion pipeline (the polled source) plus the
 * {@code direct:ingest-<pipeline>} ingress. Users never see these routes — they are the
 * implementation of the configuration.
 */
@ApplicationScoped
public class IngestRoutes extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(IngestRoutes.class);

    @Inject
    IngestBuildTimeConfig buildTimeConfig;

    @Inject
    IngestRunTimeConfig runTimeConfig;

    @Inject
    IngestMetrics metrics;

    @Inject
    IngestPipelineRegistry registry;

    private final AdoptPlan adoptPlan = new AdoptPlan();

    @Override
    public void configure() {
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : buildTimeConfig.pipelines()
                .entrySet()) {
            String name = entry.getKey();
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = entry.getValue();
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);

            if (runtime != null && !runtime.enabled()) {
                LOG.infof("Ingestion pipeline '%s' is disabled", name);
                continue;
            }

            boolean sync = "sync".equals(pipeline.mode());
            IngestionTracker tracker = sync ? createTracker(name, runtime) : null;

            EmbeddingStore<TextSegment> store = resolveStore(name, pipeline.embeddingStore().orElse(null));
            adoptPlan.add(name, pipeline.embeddingStore().orElse("<default>"), pipeline.adopt(), store);
            IngestService service = new IngestService(
                    name,
                    store,
                    resolveModel(name, pipeline.embeddingModel().orElse(null)),
                    pipeline.splitter(),
                    pipeline.maxSegmentSize(),
                    pipeline.maxOverlapSize(),
                    tracker,
                    IngestService.WriteStrategy.of(pipeline.writeStrategy()),
                    pipeline.embeddingModelId().orElse(""));

            if (runtime != null) {
                service.embeddingLimits(runtime.embedding().batchSize(),
                        runtime.embedding().requestsPerMinute().orElse(null));
            }

            // only bounded scan sources have a "first pass" to gate on
            boolean gatesReadiness = sync && (runtime == null || runtime.readiness().enabled());
            registry.register(name, service, gatesReadiness);
            switch (pipeline.source().type()) {
            case "file" -> configureSourceRoute(name, runtime, service, sync);
            default -> throw new IllegalStateException("Unknown source type " + pipeline.source().type());
            }
            configureIngressRoute(name, service);

            LOG.infof("Ingestion pipeline '%s': source=%s, mode=%s%s",
                    name, pipeline.source().type(), pipeline.mode(),
                    sync ? " (write-strategy=" + pipeline.writeStrategy() + ")"
                            : " (preview: append-only, re-ingests on restart)");
        }

        // adoption runs once, after every pipeline is declared: the shared-store wipe guard
        // needs the complete picture, and wipes must precede the first pass
        adoptPlan.execute();

        // a Camel health check: exposed wherever the app already surfaces Camel health
        // (e.g. /q/health/ready via camel-quarkus-microprofile-health)
        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(getContext());
        if (healthCheckRegistry != null) {
            healthCheckRegistry.register(new IngestReadinessCheck(registry));
        }
    }

    private IngestionTracker createTracker(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime) {
        String datasourceName = runtime == null ? null : runtime.tracker().datasource().orElse(null);
        Instance<DataSource> selected = datasourceName == null
                ? dataSources.select(Default.Literal.INSTANCE)
                : dataSources.select(new io.quarkus.agroal.DataSource.DataSourceLiteral(datasourceName));
        if (!selected.isResolvable()) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + name + "' has mode=sync, which needs a datasource for the sync "
                            + "tracker" + (datasourceName == null ? "" : " ('" + datasourceName + "')")
                            + ". Add a JDBC driver extension and configure quarkus.datasource (Dev Services "
                            + "provides one automatically in dev and test mode), or set "
                            + "quarkus.camel.ai.ingest." + name + ".mode=append");
        }
        IngestionTracker tracker = new JdbcIngestionTracker(selected.get());
        tracker.ensureSchema();
        return tracker;
    }

    private void configureSourceRoute(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            IngestService service, boolean sync) {
        String directory = runtime == null ? null : runtime.source().directory().orElse(null);
        if (directory == null) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + name + "' has source type 'file' but no directory. "
                            + "Set quarkus.camel.ai.ingest." + name + ".source.directory");
        }
        if (sync) {
            configureSyncScanRoute(name, runtime, service, directory);
        } else {
            configureAppendConsumerRoute(name, runtime, service, directory);
        }
    }

    /**
     * Sync mode runs bounded passes: enumerate the whole directory, process, then reconcile —
     * a document the tracker knows but the listing lacks has disappeared and is deleted, guarded
     * by the pass interlock (complete enumeration, zero failures, bulk-delete floor). The Camel
     * file consumer cannot signal "this listing was complete", which is why the pass is
     * timer-driven.
     */
    private void configureSyncScanRoute(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            IngestService service, String directory) {
        SyncPassRunner passRunner = new SyncPassRunner(service, service.tracker(), name,
                runtime.reconcile().bulkDeleteThreshold(), runtime.reconcile().allowBulkDelete(),
                "set quarkus.camel.ai.ingest." + name + ".reconcile.allow-bulk-delete=true for one pass");
        boolean recursive = runtime.source().recursive();
        String include = runtime.source().include().orElse(null);

        from("timer:ingest-" + name + "?period=" + runtime.source().pollInterval() + "&delay=0")
                .routeId("ingest-" + name)
                .process(exchange -> {
                    Map<String, SyncPassRunner.SourceDocument> listing;
                    try {
                        listing = enumerate(Path.of(directory), recursive, include);
                    } catch (Exception e) {
                        // enumeration incomplete: the pass must not run — deleting on a partial
                        // listing turns a failed mount into an emptied knowledge base
                        metrics.failure(name);
                        LOG.errorf(e, "Pipeline '%s': source enumeration failed — pass aborted, nothing "
                                + "processed, nothing deleted", name);
                        return;
                    }
                    SyncPassRunner.PassOutcome outcome = passRunner.run(listing);
                    metrics.applyPass(name, outcome);
                    if (outcome.succeeded()) {
                        registry.markReady(name);
                    }
                    if (outcome.ingested() + outcome.replaced() + outcome.deleted() + outcome.failed() > 0
                            || outcome.deletionRefused() > 0) {
                        LOG.infof("Pipeline '%s' pass %s: %d ingested, %d replaced, %d unchanged, %d deleted"
                                + "%s%s", name, outcome.status(), outcome.ingested(), outcome.replaced(),
                                outcome.skippedUnchanged(), outcome.deleted(),
                                outcome.failed() > 0 ? ", " + outcome.failed() + " FAILED" : "",
                                outcome.deletionRefused() > 0
                                        ? ", " + outcome.deletionRefused() + " deletions REFUSED (bulk floor)"
                                        : "");
                    }
                });
    }

    /** The complete listing of the source directory: documentId → (fingerprint, lazy content). */
    static Map<String, SyncPassRunner.SourceDocument> enumerate(Path root, boolean recursive, String include)
            throws IOException {
        Map<String, SyncPassRunner.SourceDocument> listing = new LinkedHashMap<>();
        if (!Files.isDirectory(root)) {
            return listing; // an absent directory is an empty source — the bulk floor guards mistakes
        }
        PathMatcher matcher = include == null ? null
                : root.getFileSystem().getPathMatcher("glob:" + include);
        try (Stream<Path> paths = Files.walk(root, recursive ? Integer.MAX_VALUE : 1)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path relative = root.relativize(path);
                if (matcher != null && !matcher.matches(relative)) {
                    continue;
                }
                String documentId = relative.toString().replace(File.separatorChar, '/');
                long size = Files.size(path);
                long modified = Files.getLastModifiedTime(path).toMillis();
                listing.put(documentId, new SyncPassRunner.SourceDocument(size + ":" + modified, () -> {
                    try {
                        return Files.readString(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }));
            }
        }
        return listing;
    }

    private void configureAppendConsumerRoute(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            IngestService service, String directory) {
        StringBuilder uri = new StringBuilder("file:").append(directory)
                .append("?noop=true&recursive=").append(runtime.source().recursive());
        runtime.source().include().ifPresent(include -> uri.append("&antInclude=").append(include));

        from(uri.toString())
                .routeId("ingest-" + name)
                .process(exchange -> {
                    String documentId = exchange.getMessage().getHeader(Exchange.FILE_NAME, String.class);
                    try {
                        String text = exchange.getMessage().getBody(String.class);
                        IngestResult result = service.ingest(documentId, text);
                        count(name, result);
                        LOG.debugf("Pipeline '%s', document '%s': %s (%d segment(s))", name, documentId,
                                result.outcome(), result.segmentsWritten());
                    } catch (Exception e) {
                        // on-failure=skip semantics: count, log, keep the pipeline alive
                        metrics.failure(name);
                        LOG.errorf(e, "Failed to ingest '%s' into pipeline '%s' — document skipped", documentId,
                                name);
                    }
                });
    }

    private void count(String name, IngestResult result) {
        switch (result.outcome()) {
        case REPLACED -> metrics.documentReplaced(name, result.segmentsWritten());
        case SKIPPED_UNCHANGED -> metrics.documentSkippedUnchanged(name);
        case INGESTED -> metrics.documentIngested(name, result.segmentsWritten());
        default -> {
            // empty: nothing written, nothing to count
        }
        }
    }

    private void configureIngressRoute(String name, IngestService service) {
        from("direct:ingest-" + name)
                .routeId("ingest-ingress-" + name)
                .process(exchange -> {
                    String documentId = exchange.getMessage().getHeader(IngestHeaders.DOCUMENT_ID, String.class);
                    if (documentId == null || documentId.isBlank()) {
                        metrics.failure(name);
                        throw new IllegalArgumentException(
                                "Header " + IngestHeaders.DOCUMENT_ID + " is required to ingest into pipeline '"
                                        + name + "': a stable document id is what update and delete semantics "
                                        + "of later releases build on, so it cannot be generated");
                    }
                    String fingerprint = exchange.getMessage().getHeader(IngestHeaders.FINGERPRINT, String.class);
                    String tenant = exchange.getMessage().getHeader(IngestHeaders.TENANT, String.class);
                    String text = exchange.getMessage().getBody(String.class);
                    IngestResult result = service.ingest(documentId, fingerprint, text,
                            IngestService.Origin.API, tenant);
                    count(name, result);
                    exchange.getMessage().setBody(result);
                });
    }

    private static final TypeLiteral<EmbeddingStore<TextSegment>> STORE_TYPE = new TypeLiteral<>() {
    };

    // Raw-type CDI lookups do not match EmbeddingStore<TextSegment> beans (CDI assignability
    // rules), so: named beans resolve through the Camel registry (which also covers beans the
    // @EmbeddingStoreName registry bridge contributed), unnamed resolution goes through CDI
    // with the proper TypeLiteral.
    @Inject
    @Any
    Instance<EmbeddingStore<TextSegment>> storeCandidates;

    @Inject
    @Any
    Instance<EmbeddingModel> modelCandidates;

    @Inject
    @Any
    Instance<DataSource> dataSources;

    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> resolveStore(String name, String configured) {
        if (configured != null) {
            EmbeddingStore<TextSegment> store = getContext().getRegistry().lookupByNameAndType(configured,
                    EmbeddingStore.class);
            if (store == null) {
                throw new IllegalStateException(
                        "Ingestion pipeline '" + name + "' references embedding store '" + configured
                                + "' but no such bean exists. Available: " + names(storeCandidates));
            }
            return store;
        }
        return single(name, storeCandidates, "embedding store", "embedding-store");
    }

    private EmbeddingModel resolveModel(String name, String configured) {
        if (configured != null) {
            EmbeddingModel model = getContext().getRegistry().lookupByNameAndType(configured, EmbeddingModel.class);
            if (model == null) {
                throw new IllegalStateException(
                        "Ingestion pipeline '" + name + "' references embedding model '" + configured
                                + "' but no such bean exists. Available: " + names(modelCandidates));
            }
            return model;
        }
        return single(name, modelCandidates, "embedding model", "embedding-model");
    }

    private <T> T single(String pipeline, Instance<T> candidates, String what, String property) {
        List<? extends Instance.Handle<T>> handles = candidates.handlesStream().toList();
        if (handles.size() == 1) {
            return handles.get(0).get();
        }
        throw new IllegalStateException(
                "Ingestion pipeline '" + pipeline + "' needs an " + what + ", but " + handles.size()
                        + " candidates exist" + (handles.isEmpty() ? "" : ": " + names(candidates))
                        + ". Set quarkus.camel.ai.ingest." + pipeline + "." + property);
    }

    private static String names(Instance<?> candidates) {
        return candidates.handlesStream()
                .map(handle -> {
                    String beanName = handle.getBean().getName();
                    return beanName != null ? beanName : handle.getBean().toString();
                })
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
