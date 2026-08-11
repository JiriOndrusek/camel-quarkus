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
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * One bounded synchronisation pass: process every enumerated document, then reconcile —
 * documents the ledger knows but the source no longer lists are deleted. Reconciliation is
 * ledger-versus-source; the store is never enumerated.
 *
 * <p>
 * The safety interlock, because the failure mode of getting deletion wrong is an emptied
 * knowledge base with a green log:
 * <ul>
 * <li><b>Enumeration completeness</b> — the caller only invokes this with a fully built listing;
 * a failed enumeration must abort the pass before this class is reached.</li>
 * <li><b>Zero-failure gate</b> — if any document failed to process, the pass is
 * {@code partially-failed} and deletes nothing: an undelivered document is indistinguishable
 * from a disappeared one.</li>
 * <li><b>Bulk-delete floor</b> — deleting more than the configured fraction of a pipeline's
 * source documents in one pass requires explicit consent; a mistyped directory or a failed
 * mount must be a refusal, not an incident.</li>
 * <li>Only {@code origin=source} rows are deletion candidates — API-written documents are never
 * deleted for not appearing in a listing. Tombstoned rows are suppression records, not
 * candidates.</li>
 * </ul>
 */
public class SyncPassRunner {

    private static final Logger LOG = Logger.getLogger(SyncPassRunner.class);

    /** A source document as enumerated: cheap fingerprint now, content only on demand. */
    public record SourceDocument(String fingerprint, Supplier<String> text) {
    }

    public record PassOutcome(int processed, int failed, int ingested, int replaced, int skippedUnchanged,
            int suppressed, int segmentsWritten, int deleted, int deletionRefused, String status) {
        public boolean succeeded() {
            return "succeeded".equals(status);
        }
    }

    private final IngestService service;
    private final SyncLedger ledger;
    private final String pipeline;
    private final double bulkDeleteThreshold;
    private final boolean allowBulkDelete;

    public SyncPassRunner(IngestService service, SyncLedger ledger, String pipeline,
            double bulkDeleteThreshold, boolean allowBulkDelete) {
        this.service = service;
        this.ledger = ledger;
        this.pipeline = pipeline;
        this.bulkDeleteThreshold = bulkDeleteThreshold;
        this.allowBulkDelete = allowBulkDelete;
    }

    /**
     * @param listing the complete enumeration of the source: documentId → document. The caller
     *                guarantees completeness — on any enumeration error it must not call this.
     */
    public PassOutcome run(Map<String, SourceDocument> listing) {
        int processed = 0;
        int failed = 0;
        int ingested = 0;
        int replaced = 0;
        int skippedUnchanged = 0;
        int suppressed = 0;
        int segmentsWritten = 0;

        for (Map.Entry<String, SourceDocument> entry : listing.entrySet()) {
            try {
                IngestResult result = service.ingest(entry.getKey(), entry.getValue().fingerprint(),
                        entry.getValue().text(), IngestService.Origin.SOURCE);
                processed++;
                segmentsWritten += result.segmentsWritten();
                switch (result.outcome()) {
                case IngestResult.OUTCOME_INGESTED -> ingested++;
                case IngestResult.OUTCOME_REPLACED -> replaced++;
                case IngestResult.OUTCOME_SKIPPED_UNCHANGED -> skippedUnchanged++;
                case IngestResult.OUTCOME_SUPPRESSED_TOMBSTONE, IngestResult.OUTCOME_SUPPRESSED_PINNED ->
                    suppressed++;
                default -> {
                    // empty: nothing to count
                }
                }
            } catch (Exception e) {
                failed++;
                LOG.errorf(e, "Pipeline '%s': failed to process '%s' — document skipped, deletion disabled "
                        + "for this pass", pipeline, entry.getKey());
            }
        }

        if (failed > 0) {
            return new PassOutcome(processed, failed, ingested, replaced, skippedUnchanged, suppressed,
                    segmentsWritten, 0, 0, "partially-failed");
        }

        // reconcile: ledger-versus-source
        List<SyncLedger.LedgerRow> rows = ledger.listDocuments(pipeline);
        List<SyncLedger.LedgerRow> candidates = new ArrayList<>();
        int sourceOwned = 0;
        for (SyncLedger.LedgerRow row : rows) {
            if (!SyncLedger.ORIGIN_SOURCE.equals(row.origin()) || row.tombstone()) {
                continue;
            }
            sourceOwned++;
            if (row.done() && !listing.containsKey(row.documentId())) {
                candidates.add(row);
            }
        }

        if (!candidates.isEmpty() && !allowBulkDelete
                && sourceOwned > 0 && candidates.size() > bulkDeleteThreshold * sourceOwned) {
            LOG.warnf("Pipeline '%s': refusing to delete %d of %d source documents in one pass "
                    + "(threshold %.0f%%). If this is intended (corpus restructuring), set "
                    + "quarkus.camel.ai.ingest.%s.reconcile.allow-bulk-delete=true for one pass.",
                    pipeline, candidates.size(), sourceOwned, bulkDeleteThreshold * 100, pipeline);
            return new PassOutcome(processed, 0, ingested, replaced, skippedUnchanged, suppressed,
                    segmentsWritten, 0, candidates.size(), "succeeded");
        }

        int deleted = 0;
        for (SyncLedger.LedgerRow row : candidates) {
            List<String> ids = new ArrayList<>(row.maxKnownCount());
            for (int i = 0; i < row.maxKnownCount(); i++) {
                ids.add(IngestIds.segmentId(pipeline, row.documentId(), i));
            }
            service.removeSegments(ids);
            ledger.deleteRow(pipeline, row.documentId());
            deleted++;
            LOG.infof("Pipeline '%s': document '%s' disappeared from the source — removed", pipeline,
                    row.documentId());
        }

        return new PassOutcome(processed, 0, ingested, replaced, skippedUnchanged, suppressed,
                segmentsWritten, deleted, 0, "succeeded");
    }
}
