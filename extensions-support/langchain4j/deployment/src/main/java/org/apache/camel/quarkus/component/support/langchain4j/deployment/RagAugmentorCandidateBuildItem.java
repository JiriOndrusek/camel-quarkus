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
package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A request from another extension (typically camel-quarkus-langchain4j-ingest, one per declared
 * ingestion pipeline) to produce a {@code @Named} RetrievalAugmentor backed by the given store.
 * Explicit {@code quarkus.camel.langchain4j.rag.augmentors.<name>} configuration with the same
 * name takes precedence over a candidate. Consumed only when Quarkus LangChain4j is present.
 */
public final class RagAugmentorCandidateBuildItem extends MultiBuildItem {

    private final String name;
    private final String embeddingStoreName;
    private final String embeddingModelName;

    public RagAugmentorCandidateBuildItem(String name, String embeddingStoreName, String embeddingModelName) {
        this.name = name;
        this.embeddingStoreName = embeddingStoreName;
        this.embeddingModelName = embeddingModelName;
    }

    public String getName() {
        return name;
    }

    /** May be null: the recorder then falls back to the default (unnamed) store bean. */
    public String getEmbeddingStoreName() {
        return embeddingStoreName;
    }

    /** May be null: the recorder then falls back to the default (unnamed) model bean. */
    public String getEmbeddingModelName() {
        return embeddingModelName;
    }
}
