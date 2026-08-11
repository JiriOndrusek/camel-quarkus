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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.jboss.logging.Logger;

/**
 * Store adoption, executed once before routes start. Adoption is a <em>declaration</em>, not a
 * detection: the store cannot be enumerated, so "fail if foreign vectors exist" cannot be
 * implemented — a top-k probe is a sample, not a proof.
 *
 * <p>
 * {@code wipe} is store-wide by nature ({@code removeAll()}), so it is refused when several
 * pipelines share the store: one pipeline's fresh start must not destroy its siblings' corpora.
 */
public final class AdoptPlan {

    private static final Logger LOG = Logger.getLogger(AdoptPlan.class);

    public record Entry(String pipeline, String storeKey, String adoptMode,
            EmbeddingStore<TextSegment> store) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public void add(String pipeline, String storeKey, String adoptMode, EmbeddingStore<TextSegment> store) {
        if (!"assume-empty".equals(adoptMode) && !"wipe".equals(adoptMode) && !"coexist".equals(adoptMode)) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + pipeline + "' has unknown adopt mode '" + adoptMode
                            + "'. Supported: assume-empty, wipe, coexist");
        }
        entries.add(new Entry(pipeline, storeKey, adoptMode, store));
    }

    /** Validates the plan and executes wipes. Call after all pipelines are declared. */
    public void execute() {
        Map<String, List<Entry>> byStore = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byStore.computeIfAbsent(entry.storeKey(), k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<Entry>> group : byStore.entrySet()) {
            List<Entry> sharing = group.getValue();
            boolean anyWipe = sharing.stream().anyMatch(e -> "wipe".equals(e.adoptMode()));
            if (anyWipe && sharing.size() > 1) {
                throw new IllegalStateException(
                        "adopt=wipe on store '" + group.getKey() + "' is refused: " + sharing.size()
                                + " pipelines share this store ("
                                + sharing.stream().map(Entry::pipeline).sorted().reduce((a, b) -> a + ", " + b)
                                        .orElse("")
                                + ") and removeAll() is store-wide — one pipeline's fresh start must not "
                                + "destroy its siblings' corpora. Give the wiping pipeline its own store.");
            }
        }

        for (Entry entry : entries) {
            switch (entry.adoptMode()) {
            case "wipe" -> {
                LOG.warnf("Pipeline '%s': adopt=wipe — removing ALL vectors from store '%s' before "
                        + "ingesting. This is the only adoption mode that can guarantee an exact mirror "
                        + "of the source.", entry.pipeline(), entry.storeKey());
                entry.store().removeAll();
            }
            case "assume-empty" -> LOG.infof(
                    "Pipeline '%s': adopt=assume-empty — ASSERTING store '%s' holds no foreign vectors. "
                            + "Pre-existing content cannot be detected or replaced; if the store was "
                            + "populated before, expect duplicates (use adopt=wipe or a fresh store).",
                    entry.pipeline(), entry.storeKey());
            case "coexist" -> LOG.infof(
                    "Pipeline '%s': adopt=coexist — foreign vectors in store '%s' are never replaced or "
                            + "removed.",
                    entry.pipeline(), entry.storeKey());
            default -> throw new IllegalStateException("unreachable");
            }
        }
    }
}
