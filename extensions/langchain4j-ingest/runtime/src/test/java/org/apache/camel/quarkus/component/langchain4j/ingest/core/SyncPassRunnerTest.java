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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deletion interlock (100/p4-plan.md): a disappeared document is removed, but never on a
 * partially-failed pass, never past the bulk-delete floor, never for API-origin documents, and
 * tombstones/pins survive passes.
 */
class SyncPassRunnerTest {

    SyncLedger ledger;
    IngestSyncProtocolTest.UpsertFakeStore store;
    IngestService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ledger = new JdbcSyncLedger(dataSource);
        ledger.ensureSchema();
        store = new IngestSyncProtocolTest.UpsertFakeStore();
        service = new IngestService("p", store, new IngestSyncProtocolTest.FakeModel(), "none", 500, 50,
                ledger, IngestService.WriteStrategy.UPSERT, "m1");
    }

    SyncPassRunner runner(double threshold, boolean allowBulk) {
        return new SyncPassRunner(service, ledger, "p", threshold, allowBulk);
    }

    static Map<String, SyncPassRunner.SourceDocument> listing(String... idAndContentPairs) {
        Map<String, SyncPassRunner.SourceDocument> listing = new LinkedHashMap<>();
        for (int i = 0; i < idAndContentPairs.length; i += 2) {
            String content = idAndContentPairs[i + 1];
            listing.put(idAndContentPairs[i],
                    new SyncPassRunner.SourceDocument("fp-" + content.hashCode(), () -> content));
        }
        return listing;
    }

    @Test
    void disappearedDocumentIsDeleted() {
        runner(0.9, false).run(listing("a.txt", "content a", "b.txt", "content b"));
        assertEquals(2, store.entries.size());

        SyncPassRunner.PassOutcome outcome = runner(0.9, false).run(listing("a.txt", "content a"));
        assertEquals(1, outcome.deleted());
        assertEquals(1, store.entries.size(), "b.txt segments must be removed");
        assertTrue(ledger.read("p", "b.txt").isEmpty(), "b.txt row must be gone");
    }

    @Test
    void partiallyFailedPassDeletesNothing() {
        runner(0.9, false).run(listing("a.txt", "content a", "b.txt", "content b"));

        Map<String, SyncPassRunner.SourceDocument> failing = new LinkedHashMap<>();
        failing.put("a.txt", new SyncPassRunner.SourceDocument("fp-x", () -> {
            throw new IllegalStateException("simulated read failure");
        }));
        // b.txt is missing from the listing AND a.txt fails: nothing may be deleted
        SyncPassRunner.PassOutcome outcome = runner(0.9, false).run(failing);

        assertEquals("partially-failed", outcome.status());
        assertEquals(0, outcome.deleted());
        assertEquals(2, store.entries.size(), "an undelivered document is indistinguishable from a "
                + "disappeared one — nothing may be deleted");
        assertTrue(ledger.read("p", "b.txt").isPresent());
    }

    @Test
    void bulkDeleteFloorRefusesWithoutConsentAndObeysWithIt() {
        runner(0.9, false).run(listing("a.txt", "content a", "b.txt", "content b", "c.txt", "content c"));

        // everything disappeared — way past a 10% threshold
        SyncPassRunner.PassOutcome refused = runner(0.1, false).run(listing());
        assertEquals(0, refused.deleted());
        assertEquals(3, refused.deletionRefused());
        assertEquals(3, store.entries.size(), "mass disappearance without consent must be refused");

        SyncPassRunner.PassOutcome consented = runner(0.1, true).run(listing());
        assertEquals(3, consented.deleted());
        assertEquals(0, store.entries.size());
    }

    @Test
    void apiOriginDocumentsSurviveReconciliation() {
        service.ingest("api-doc", null, "an admin correction", IngestService.Origin.API);
        runner(0.9, false).run(listing("a.txt", "content a"));

        SyncPassRunner.PassOutcome outcome = runner(0.9, false).run(listing("a.txt", "content a"));
        assertEquals(0, outcome.deleted(), "api-origin documents never appear in listings and must "
                + "never be deleted for it");
        assertTrue(ledger.read("p", "api-doc").isPresent());
    }

    @Test
    void tombstoneSurvivesPassesAndUnsuppressLifts() {
        runner(0.9, false).run(listing("a.txt", "content a"));
        service.delete("a.txt");
        assertEquals(0, store.entries.size());

        // the file is still in the listing — the tombstone must keep it out
        SyncPassRunner.PassOutcome pass = runner(0.9, false).run(listing("a.txt", "content a"));
        assertEquals(1, pass.suppressed());
        assertEquals(0, store.entries.size(), "delete must survive the next pass");

        service.unsuppress("a.txt");
        runner(0.9, false).run(listing("a.txt", "content a"));
        assertEquals(1, store.entries.size(), "unsuppress must hand the document back to the source");
    }

    @Test
    void apiUpsertOverSourceDocumentPinsAndUnpinReleases() {
        runner(0.9, false).run(listing("a.txt", "source version"));

        service.ingest("a.txt", null, "corrected version", IngestService.Origin.API);
        assertTrue(ledger.read("p", "a.txt").orElseThrow().pinned(), "api write over a source doc must pin");

        runner(0.9, false).run(listing("a.txt", "source version"));
        assertEquals("corrected version", store.entries.values().iterator().next().text(),
                "the pinned correction must survive the next source pass");

        service.unpin("a.txt");
        runner(0.9, false).run(listing("a.txt", "source version"));
        assertEquals("source version", store.entries.values().iterator().next().text(),
                "unpin must hand the document back to the source");
    }

    @Test
    void pinnedDocumentIsNotDeletedWhenItsSourceFileDisappears() {
        runner(0.9, false).run(listing("a.txt", "source version"));
        service.ingest("a.txt", null, "corrected version", IngestService.Origin.API);

        // origin flipped to api by the correction; the file disappearing must not delete it
        SyncPassRunner.PassOutcome outcome = runner(0.9, false).run(listing());
        assertEquals(0, outcome.deleted());
        assertFalse(store.entries.isEmpty(), "the pinned correction must survive source disappearance");
    }
}
