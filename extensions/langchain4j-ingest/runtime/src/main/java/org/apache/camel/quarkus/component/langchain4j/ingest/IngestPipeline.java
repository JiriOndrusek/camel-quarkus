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

    String sourceUri() {
        return source.uri();
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
