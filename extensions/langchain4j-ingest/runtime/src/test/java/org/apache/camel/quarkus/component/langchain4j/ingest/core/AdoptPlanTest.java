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

import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdoptPlanTest {

    static class WipeCountingStore implements EmbeddingStore<TextSegment> {
        int wipes;

        @Override
        public void removeAll() {
            wipes++;
        }

        @Override
        public String add(Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(String id, Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String add(Embedding embedding, TextSegment segment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return new EmbeddingSearchResult<>(List.of());
        }
    }

    @Test
    void wipeOnExclusiveStoreExecutes() {
        WipeCountingStore store = new WipeCountingStore();
        AdoptPlan plan = new AdoptPlan();
        plan.add("a", "store-a", "wipe", store);
        plan.add("b", "store-b", "assume-empty", new WipeCountingStore());
        plan.execute();
        assertEquals(1, store.wipes);
    }

    @Test
    void wipeOnSharedStoreIsRefused() {
        WipeCountingStore shared = new WipeCountingStore();
        AdoptPlan plan = new AdoptPlan();
        plan.add("a", "shared", "wipe", shared);
        plan.add("b", "shared", "coexist", shared);
        IllegalStateException e = assertThrows(IllegalStateException.class, plan::execute);
        assertTrue(e.getMessage().contains("store-wide"), e.getMessage());
        assertEquals(0, shared.wipes, "nothing may be wiped when the plan is refused");
    }

    @Test
    void unknownModeIsRefusedAtDeclaration() {
        AdoptPlan plan = new AdoptPlan();
        assertThrows(IllegalStateException.class,
                () -> plan.add("a", "s", "detect", new WipeCountingStore()));
    }
}
