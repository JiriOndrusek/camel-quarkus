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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.JdbcSyncLedger;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.SyncLedger;
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
            SyncLedger ledger = sync ? createLedger(name, runtime) : null;

            IngestService service = new IngestService(
                    name,
                    resolveStore(name, pipeline),
                    resolveModel(name, pipeline),
                    pipeline.splitter(),
                    pipeline.maxSegmentSize(),
                    pipeline.maxOverlapSize(),
                    ledger,
                    IngestService.WriteStrategy.of(pipeline.writeStrategy()),
                    pipeline.embeddingModelId().orElse(""));

            configureSourceRoute(name, runtime, service, sync);
            configureIngressRoute(name, service);

            LOG.infof("Ingestion pipeline '%s': source=%s, mode=%s%s",
                    name, pipeline.source().type(), pipeline.mode(),
                    sync ? " (write-strategy=" + pipeline.writeStrategy() + ")"
                            : " (preview: append-only, re-ingests on restart)");
        }
    }

    private SyncLedger createLedger(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime) {
        String datasourceName = runtime == null ? null : runtime.ledger().datasource().orElse(null);
        Instance<DataSource> selected = datasourceName == null
                ? dataSources.select(Default.Literal.INSTANCE)
                : dataSources.select(new io.quarkus.agroal.DataSource.DataSourceLiteral(datasourceName));
        if (!selected.isResolvable()) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + name + "' has mode=sync, which needs a datasource for the sync "
                            + "ledger" + (datasourceName == null ? "" : " ('" + datasourceName + "')")
                            + ". Add a JDBC driver extension and configure quarkus.datasource (Dev Services "
                            + "provides one automatically in dev and test mode), or set "
                            + "quarkus.camel.ai.ingest." + name + ".mode=append");
        }
        SyncLedger ledger = new JdbcSyncLedger(selected.get());
        ledger.ensureSchema();
        return ledger;
    }

    private void configureSourceRoute(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            IngestService service, boolean sync) {
        String directory = runtime == null ? null : runtime.source().directory().orElse(null);
        if (directory == null) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + name + "' has source type 'file' but no directory. "
                            + "Set quarkus.camel.ai.ingest." + name + ".source.directory");
        }
        StringBuilder uri = new StringBuilder("file:").append(directory)
                .append("?noop=true&recursive=").append(runtime.source().recursive());
        runtime.source().include().ifPresent(include -> uri.append("&antInclude=").append(include));
        if (sync) {
            // a changed file must be REdelivered within the same runtime: keying idempotency on
            // name+mtime makes an edit a new key, while the ledger still skips true no-changes
            uri.append("&idempotentKey=${file:name}-${file:modified}");
        }

        from(uri.toString())
                .routeId("ingest-" + name)
                .process(exchange -> {
                    String documentId = exchange.getMessage().getHeader(Exchange.FILE_NAME, String.class);
                    try {
                        String fingerprint = fileFingerprint(exchange);
                        String text = exchange.getMessage().getBody(String.class);
                        IngestResult result = service.ingest(documentId, fingerprint, text);
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

    /** Tier-1 fingerprint of a file: size + last-modified, available without reading content. */
    private static String fileFingerprint(Exchange exchange) {
        Long length = exchange.getMessage().getHeader(Exchange.FILE_LENGTH, Long.class);
        Long modified = exchange.getMessage().getHeader(Exchange.FILE_LAST_MODIFIED, Long.class);
        return length == null || modified == null ? null : length + ":" + modified;
    }

    private void count(String name, IngestResult result) {
        switch (result.outcome()) {
        case IngestResult.OUTCOME_REPLACED -> metrics.documentReplaced(name, result.segmentsWritten());
        case IngestResult.OUTCOME_SKIPPED_UNCHANGED -> metrics.documentSkippedUnchanged(name);
        case IngestResult.OUTCOME_INGESTED -> metrics.documentIngested(name, result.segmentsWritten());
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
                    String text = exchange.getMessage().getBody(String.class);
                    IngestResult result = service.ingest(documentId, fingerprint, text);
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
    private EmbeddingStore<TextSegment> resolveStore(String name, IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline) {
        String configured = pipeline.embeddingStore().orElse(null);
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

    private EmbeddingModel resolveModel(String name, IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline) {
        String configured = pipeline.embeddingModel().orElse(null);
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
