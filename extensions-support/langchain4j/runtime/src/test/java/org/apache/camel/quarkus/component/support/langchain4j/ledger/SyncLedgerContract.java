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
package org.apache.camel.quarkus.component.support.langchain4j.ledger;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The behavioural contract every {@link SyncLedger} implementation must satisfy. The sync
 * protocol of the ingestion extension is verified against these
 * guarantees, so a replacement implementation — e.g. an adapter over a future upstream
 * LangChain4j record manager (langchain4j#2931) — is a drop-in exactly when its subclass of
 * this test passes.
 */
public abstract class SyncLedgerContract {

    protected SyncLedger ledger;

    /** A fresh, empty ledger per test. */
    protected abstract SyncLedger createLedger();

    @BeforeEach
    void setUpLedger() {
        ledger = createLedger();
        ledger.ensureSchema();
    }

    @Test
    void unknownDocumentReadsEmpty() {
        assertTrue(ledger.read("p", "missing").isEmpty());
    }

    @Test
    void intentIsDurableAndNeverSkippable() {
        ledger.writeIntent("p", "doc", "fp1", "hash1", 0, 5, SyncLedger.ORIGIN_SOURCE);

        SyncLedger.LedgerRow row = ledger.read("p", "doc").orElseThrow();
        assertFalse(row.done(), "an intent row must not count as done");
        assertEquals(5, row.maxKnownCount(), "the shrink bound must cover the intended count");
    }

    @Test
    void commitCompletesTheIntent() {
        ledger.writeIntent("p", "doc", "fp1", "hash1", 0, 5, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp1", "hash1", 3);

        SyncLedger.LedgerRow row = ledger.read("p", "doc").orElseThrow();
        assertTrue(row.done());
        assertEquals("fp1", row.fingerprint());
        assertEquals("hash1", row.contentHash());
        assertEquals(3, row.segmentCount());
    }

    @Test
    void reintentKeepsTheLargestKnownCount() {
        ledger.writeIntent("p", "doc", "fp1", "hash1", 0, 5, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp1", "hash1", 5);
        ledger.writeIntent("p", "doc", "fp2", "hash2", 5, 2, SyncLedger.ORIGIN_SOURCE);

        assertEquals(5, ledger.read("p", "doc").orElseThrow().maxKnownCount(),
                "shrink must still see the previously committed tail");
    }

    @Test
    void refreshFingerprintTouchesNothingElse() {
        ledger.writeIntent("p", "doc", "fp1", "hash1", 0, 2, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp1", "hash1", 2);
        ledger.refreshFingerprint("p", "doc", "fp2");

        SyncLedger.LedgerRow row = ledger.read("p", "doc").orElseThrow();
        assertEquals("fp2", row.fingerprint());
        assertEquals("hash1", row.contentHash());
        assertEquals(2, row.segmentCount());
        assertTrue(row.done());
    }

    @Test
    void listDocumentsIsIsolatedByPipeline() {
        ledger.writeIntent("p1", "a", "fp", "h", 0, 1, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p1", "a", "fp", "h", 1);
        ledger.writeIntent("p2", "b", "fp", "h", 0, 1, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p2", "b", "fp", "h", 1);

        List<SyncLedger.LedgerRow> rows = ledger.listDocuments("p1");
        assertEquals(1, rows.size());
        assertEquals("a", rows.get(0).documentId());
    }

    @Test
    void deleteRowForgetsTheDocument() {
        ledger.writeIntent("p", "doc", "fp", "h", 0, 1, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp", "h", 1);
        ledger.deleteRow("p", "doc");

        assertTrue(ledger.read("p", "doc").isEmpty());
        assertTrue(ledger.listDocuments("p").isEmpty());
    }

    @Test
    void tombstoneSurvivesAndLifts() {
        ledger.writeIntent("p", "doc", "fp", "h", 0, 1, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp", "h", 1);

        ledger.tombstone("p", "doc");
        assertTrue(ledger.read("p", "doc").orElseThrow().tombstone(),
                "a tombstoned row must survive as a suppression record");

        ledger.unsuppress("p", "doc");
        assertFalse(ledger.read("p", "doc").orElseThrow().tombstone());
    }

    @Test
    void pinSurvivesAndLifts() {
        ledger.writeIntent("p", "doc", "fp", "h", 0, 1, SyncLedger.ORIGIN_API);
        ledger.commit("p", "doc", "fp", "h", 1);

        ledger.pin("p", "doc");
        assertTrue(ledger.read("p", "doc").orElseThrow().pinned());

        ledger.unpin("p", "doc");
        assertFalse(ledger.read("p", "doc").orElseThrow().pinned());
    }

    @Test
    void markFailedRecordsTheAttemptAndKeepsCommittedState() {
        ledger.writeIntent("p", "doc", "fp1", "h1", 0, 2, SyncLedger.ORIGIN_SOURCE);
        ledger.commit("p", "doc", "fp1", "h1", 2);
        ledger.markFailed("p", "doc", "fp2");

        SyncLedger.LedgerRow row = ledger.read("p", "doc").orElseThrow();
        assertTrue(row.failed());
        assertEquals("fp2", row.fingerprint(), "the failed attempt's fingerprint gates retries");
        assertEquals(2, row.segmentCount(), "previously committed segments keep serving");
    }
}
