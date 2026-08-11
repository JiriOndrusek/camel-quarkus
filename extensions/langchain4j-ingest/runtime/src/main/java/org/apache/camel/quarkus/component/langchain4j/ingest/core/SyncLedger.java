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

import java.util.Optional;

/**
 * The sync ledger: one row per document, the authority on what was ingested. The vector store is
 * a projection that is never asked questions — losing the ledger costs re-ingestion (which
 * converges thanks to deterministic segment ids), never correctness.
 */
public interface SyncLedger {

    /** Creates or migrates the backing schema. Called once before first use. */
    void ensureSchema();

    Optional<LedgerRow> read(String pipeline, String documentId);

    /**
     * Durably records the intent to (re)write a document <em>before</em> the store is touched.
     * A row left {@code in_progress} by a crash is never skipped by change detection, so the
     * next delivery of the document converges the store.
     */
    void writeIntent(String pipeline, String documentId, String fingerprint, String contentHash,
            int committedCount, int intendedCount);

    /** Marks the write complete. Only rows in {@code done} status participate in skip decisions. */
    void commit(String pipeline, String documentId, String fingerprint, String contentHash, int segmentCount);

    /**
     * Refreshes the stored fingerprint of an already-{@code done} row whose content turned out
     * unchanged (tier-2 hash match after a tier-1 fingerprint miss), so the cheaper tier-1 check
     * skips the document next time.
     */
    void refreshFingerprint(String pipeline, String documentId, String fingerprint);

    /**
     * @param status {@code done} or {@code in_progress}
     */
    record LedgerRow(String pipeline, String documentId, String fingerprint, String contentHash,
            int segmentCount, int intendedCount, String status) {

        public boolean done() {
            return "done".equals(status);
        }

        /** The shrink bound: the largest segment index that may exist in the store, ever intended. */
        public int maxKnownCount() {
            return Math.max(segmentCount, intendedCount);
        }
    }
}
