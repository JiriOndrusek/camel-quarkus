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
package org.apache.camel.quarkus.component.langchain4j.ingest.core;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Ingests one document into one embedding store: split, embed, write with deterministic segment
 * ids. Deliberately free of any Camel dependency — this package is the future sync-engine core.
 * Current release is append-mode only: no change detection, no replace, no deletion.
 */
public class IngestService {

    private final String pipeline;
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel model;
    private final DocumentSplitter splitter;

    public IngestService(String pipeline, EmbeddingStore<TextSegment> store, EmbeddingModel model,
            String splitterKind, int maxSegmentSize, int maxOverlapSize) {
        this.pipeline = pipeline;
        this.store = store;
        this.model = model;
        this.splitter = switch (splitterKind) {
        case "recursive" -> DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
        case "none" -> null;
        default -> throw new IllegalArgumentException(
                "Unknown splitter '" + splitterKind + "' for ingestion pipeline '" + pipeline
                        + "'. Supported: recursive, none");
        };
    }

    public IngestResult ingest(String documentId, String text) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException(
                    "A stable document id is required to ingest into pipeline '" + pipeline + "'");
        }
        if (text == null || text.isBlank()) {
            return new IngestResult(pipeline, documentId, 0);
        }

        List<TextSegment> segments = splitter == null
                ? List.of(TextSegment.from(text))
                : splitter.split(Document.from(text));

        List<Embedding> embeddings = model.embedAll(segments).content();

        List<String> ids = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            ids.add(IngestIds.segmentId(pipeline, documentId, i));
        }
        store.addAll(ids, embeddings, segments);
        return new IngestResult(pipeline, documentId, segments.size());
    }

    public String pipeline() {
        return pipeline;
    }
}
