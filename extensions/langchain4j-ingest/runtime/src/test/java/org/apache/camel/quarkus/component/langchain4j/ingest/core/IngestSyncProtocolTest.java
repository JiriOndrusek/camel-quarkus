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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sync protocol invariants, at the core level: skip tiers, replace without duplicates,
 * shrink, both write strategies, and crash convergence — the properties the P1 chaos test proved
 * for the prototype, now pinned on the real implementation.
 */
class IngestSyncProtocolTest {

    JdbcDataSource dataSource;
    SyncLedger ledger;
    UpsertFakeStore store;
    FakeModel model = new FakeModel();

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ledger = new JdbcSyncLedger(dataSource);
        ledger.ensureSchema();
        store = new UpsertFakeStore();
    }

    IngestService syncService(IngestService.WriteStrategy strategy, EmbeddingStore<TextSegment> targetStore) {
        return new IngestService("p", targetStore, model, "none", 500, 50, ledger, strategy, "model-1");
    }

    @Test
    void ingestThenUnchangedFingerprintSkipsWithoutStoreAccess() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);

        IngestResult first = service.ingest("doc", "fp1", "hello world");
        assertEquals(IngestResult.OUTCOME_INGESTED, first.outcome());
        assertEquals(1, store.entries.size());
        int writesAfterFirst = store.writeCalls;

        IngestResult second = service.ingest("doc", "fp1", "hello world");
        assertEquals(IngestResult.OUTCOME_SKIPPED_UNCHANGED, second.outcome());
        assertEquals(writesAfterFirst, store.writeCalls, "tier-1 skip must not touch the store");
    }

    @Test
    void changedFingerprintSameContentSkipsAndRefreshesFingerprint() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "hello world");

        IngestResult second = service.ingest("doc", "fp2", "hello world");
        assertEquals(IngestResult.OUTCOME_SKIPPED_UNCHANGED, second.outcome());
        assertEquals("fp2", ledger.read("p", "doc").orElseThrow().fingerprint(),
                "tier-2 skip must refresh the fingerprint so tier-1 works next time");

        int writes = store.writeCalls;
        IngestResult third = service.ingest("doc", "fp2", "hello world");
        assertEquals(IngestResult.OUTCOME_SKIPPED_UNCHANGED, third.outcome());
        assertEquals(writes, store.writeCalls);
    }

    @Test
    void changedContentReplacesInPlaceWithoutDuplicates() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "old content");

        IngestResult result = service.ingest("doc", "fp2", "new content");
        assertEquals(IngestResult.OUTCOME_REPLACED, result.outcome());
        assertEquals(1, store.entries.size(), "same deterministic id must be overwritten, not duplicated");
        assertEquals("new content", store.entries.values().iterator().next().text());
    }

    @Test
    void shrinkRemovesStaleTailSegments() {
        // splitter=none gives one segment per ingest; emulate multi-segment via a splitting service
        IngestService service = new IngestService("p", store, model, "recursive", 12, 0, ledger,
                IngestService.WriteStrategy.UPSERT, "model-1");

        service.ingest("doc", "fp1", "aaaa bbbb cccc dddd eeee");
        int before = store.entries.size();
        assertTrue(before > 1, "expected multiple segments, got " + before);

        IngestResult result = service.ingest("doc", "fp2", "tiny");
        assertEquals(IngestResult.OUTCOME_REPLACED, result.outcome());
        assertEquals(1, store.entries.size(), "stale tail segments must be removed on shrink");
    }

    @Test
    void removeThenAddStrategyConvergesOnDuplicatingStore() {
        DuplicatingFakeStore duplicating = new DuplicatingFakeStore();
        IngestService service = syncService(IngestService.WriteStrategy.REMOVE_THEN_ADD, duplicating);

        service.ingest("doc", "fp1", "old content");
        service.ingest("doc", "fp2", "new content");

        assertEquals(1, duplicating.entriesList().size(),
                "remove-then-add must clear the old generation on a store whose write duplicates");
        assertEquals("new content", duplicating.entriesList().get(0).text());
    }

    @Test
    void crashAfterIntentConvergesOnNextDelivery() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "content v1");

        // simulate a crash: intent written (larger intended count), store partially written, no commit
        ledger.writeIntent("p", "doc", "fp2", "whatever", 1, 5, SyncLedger.ORIGIN_SOURCE);

        // same fingerprint as the crashed attempt: an in_progress row must never be skipped
        IngestResult result = service.ingest("doc", "fp2", "content v2");
        assertEquals(IngestResult.OUTCOME_REPLACED, result.outcome());
        assertTrue(ledger.read("p", "doc").orElseThrow().done());
        assertEquals(1, store.entries.size());
        assertEquals("content v2", store.entries.values().iterator().next().text());
    }

    // --- fakes -------------------------------------------------------------------------------

    /** Same-id write overwrites — the pgvector/qdrant/elasticsearch behaviour from the P1 matrix. */
    static class UpsertFakeStore implements EmbeddingStore<TextSegment> {
        final Map<String, TextSegment> entries = new LinkedHashMap<>();
        int writeCalls;

        @Override
        public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
            writeCalls++;
            for (int i = 0; i < ids.size(); i++) {
                entries.put(ids.get(i), segments.get(i));
            }
        }

        @Override
        public void removeAll(java.util.Collection<String> ids) {
            ids.forEach(entries::remove);
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

    /** Same-id write appends another row — the chroma/milvus/in-memory behaviour. */
    static class DuplicatingFakeStore extends UpsertFakeStore {
        final List<TextSegment> rows = new ArrayList<>();
        final List<String> rowIds = new ArrayList<>();

        @Override
        public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
            writeCalls++;
            for (int i = 0; i < ids.size(); i++) {
                rowIds.add(ids.get(i));
                rows.add(segments.get(i));
            }
            rebuild();
        }

        @Override
        public void removeAll(java.util.Collection<String> ids) {
            for (int i = rowIds.size() - 1; i >= 0; i--) {
                if (ids.contains(rowIds.get(i))) {
                    rowIds.remove(i);
                    rows.remove(i);
                }
            }
            rebuild();
        }

        List<TextSegment> entriesList() {
            return rows;
        }

        private void rebuild() {
            entries.clear();
            for (int i = 0; i < rowIds.size(); i++) {
                // duplicate ids collapse in the map, so expose the raw rows for assertions
                entries.put(rowIds.get(i) + "#" + i, rows.get(i));
            }
        }
    }

    static class FakeModel implements EmbeddingModel {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<>(segments.size());
            for (TextSegment segment : segments) {
                out.add(embeddingFor(segment.text()));
            }
            return Response.from(out);
        }

        static Embedding embeddingFor(String text) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
                Random random = new Random(digest[0]);
                float[] vector = new float[8];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = random.nextFloat();
                }
                return new Embedding(vector);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
