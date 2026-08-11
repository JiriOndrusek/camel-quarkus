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

import java.util.Optional;

/**
 * A builder-declared ingestion pipeline — the type-safe twin of the
 * {@code quarkus.camel.ai.ingest.*} configuration. Returned from {@link Ingest @Ingest} methods.
 */
public final class IngestPipeline {

    private final Source source;
    private String embeddingStore;
    private String embeddingModel;
    private String embeddingModelId = "";
    private String mode = "append";
    private String writeStrategy = "upsert";
    private String splitter = "recursive";
    private int maxSegmentSize = 500;
    private int maxOverlapSize = 50;
    private String ledgerDatasource;
    private double bulkDeleteThreshold = 0.1;
    private boolean allowBulkDelete;
    private boolean readinessEnabled = true;
    private Boolean leaderOnly;
    private int embeddingBatchSize = 32;
    private Integer embeddingRequestsPerMinute;
    private String adopt = "assume-empty";

    private IngestPipeline(Source source) {
        this.source = source;
    }

    public static IngestPipeline from(Source source) {
        return new IngestPipeline(source);
    }

    /** The {@code EmbeddingStore} bean name; may be omitted when exactly one exists. */
    public IngestPipeline embeddingStore(String beanName) {
        this.embeddingStore = beanName;
        return this;
    }

    /** The {@code EmbeddingModel} bean name; may be omitted when exactly one exists. */
    public IngestPipeline embeddingModel(String beanName) {
        this.embeddingModel = beanName;
        return this;
    }

    /** User-declared model identity, folded into change detection. */
    public IngestPipeline embeddingModelId(String id) {
        this.embeddingModelId = id;
        return this;
    }

    /** Synchronisation mode: replace, restart-safety, deletion. Needs a datasource. */
    public IngestPipeline sync() {
        this.mode = "sync";
        return this;
    }

    /** {@code upsert} or {@code remove-then-add}, by the store's measured same-id semantics. */
    public IngestPipeline writeStrategy(String strategy) {
        this.writeStrategy = strategy;
        return this;
    }

    public IngestPipeline splitter(String kind, int maxSegmentSize, int maxOverlapSize) {
        this.splitter = kind;
        this.maxSegmentSize = maxSegmentSize;
        this.maxOverlapSize = maxOverlapSize;
        return this;
    }

    /** Name of the datasource backing the sync ledger; the default datasource when unset. */
    public IngestPipeline ledgerDatasource(String datasourceName) {
        this.ledgerDatasource = datasourceName;
        return this;
    }

    public IngestPipeline bulkDeleteThreshold(double threshold) {
        this.bulkDeleteThreshold = threshold;
        return this;
    }

    public IngestPipeline allowBulkDelete(boolean allow) {
        this.allowBulkDelete = allow;
        return this;
    }

    public IngestPipeline readinessEnabled(boolean enabled) {
        this.readinessEnabled = enabled;
        return this;
    }

    public IngestPipeline leaderOnly(boolean leaderOnly) {
        this.leaderOnly = leaderOnly;
        return this;
    }

    public IngestPipeline embeddingBatchSize(int batchSize) {
        this.embeddingBatchSize = batchSize;
        return this;
    }

    public IngestPipeline embeddingRequestsPerMinute(int requestsPerMinute) {
        this.embeddingRequestsPerMinute = requestsPerMinute;
        return this;
    }

    /** {@code assume-empty}, {@code wipe} or {@code coexist} — see the configuration docs. */
    public IngestPipeline adopt(String adoptMode) {
        this.adopt = adoptMode;
        return this;
    }

    String sourceUri() {
        return source.uri();
    }

    String sourceType() {
        return source.type();
    }

    boolean readinessEnabledValue() {
        return readinessEnabled;
    }

    String adoptValue() {
        return adopt;
    }

    /** The runtime-config view, so builder pipelines reuse every existing route configurator. */
    IngestRunTimeConfig.PipelineRunTimeConfig asRunTimeConfig() {
        IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig sourceConfig = source.asRunTimeConfig();
        return new IngestRunTimeConfig.PipelineRunTimeConfig() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public SourceRunTimeConfig source() {
                return sourceConfig;
            }

            @Override
            public LedgerRunTimeConfig ledger() {
                return () -> Optional.ofNullable(ledgerDatasource);
            }

            @Override
            public ReconcileRunTimeConfig reconcile() {
                return new ReconcileRunTimeConfig() {
                    @Override
                    public double bulkDeleteThreshold() {
                        return bulkDeleteThreshold;
                    }

                    @Override
                    public boolean allowBulkDelete() {
                        return allowBulkDelete;
                    }
                };
            }

            @Override
            public ReadinessRunTimeConfig readiness() {
                return () -> readinessEnabled;
            }

            @Override
            public Optional<Boolean> leaderOnly() {
                return Optional.ofNullable(leaderOnly);
            }

            @Override
            public EmbeddingRunTimeConfig embedding() {
                return new EmbeddingRunTimeConfig() {
                    @Override
                    public int batchSize() {
                        return embeddingBatchSize;
                    }

                    @Override
                    public Optional<Integer> requestsPerMinute() {
                        return Optional.ofNullable(embeddingRequestsPerMinute);
                    }
                };
            }
        };
    }

    Optional<String> embeddingStoreName() {
        return Optional.ofNullable(embeddingStore);
    }

    Optional<String> embeddingModelName() {
        return Optional.ofNullable(embeddingModel);
    }

    String embeddingModelIdValue() {
        return embeddingModelId;
    }

    String mode() {
        return mode;
    }

    String writeStrategyValue() {
        return writeStrategy;
    }

    String splitterKind() {
        return splitter;
    }

    int maxSegmentSize() {
        return maxSegmentSize;
    }

    int maxOverlapSize() {
        return maxOverlapSize;
    }
}
