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

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.SyncPassRunner;

/**
 * The per-pipeline ingestion counters. "It fails loudly" is a first-release promise, so these
 * exist unconditionally; every increment is also forwarded to any {@link IngestMetricsListener}
 * beans — with Micrometer present, the counters appear as {@code cq.ingest.*} meters.
 */
@Singleton
public class IngestMetrics {

    private final ConcurrentMap<String, LongAdder> documents = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> segments = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> replaced = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> skippedUnchanged = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> deleted = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> deadLettered = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> staleRetained = new ConcurrentHashMap<>();

    @Inject
    @Any
    Instance<IngestMetricsListener> listeners;

    public void documentIngested(String pipeline, int segmentsWritten) {
        record(documents, "documents", pipeline, 1);
        record(segments, "segments", pipeline, segmentsWritten);
    }

    public void documentReplaced(String pipeline, int segmentsWritten) {
        documentIngested(pipeline, segmentsWritten);
        record(replaced, "replaced", pipeline, 1);
    }

    public void documentSkippedUnchanged(String pipeline) {
        record(skippedUnchanged, "skipped-unchanged", pipeline, 1);
    }

    public void failure(String pipeline) {
        record(failures, "failures", pipeline, 1);
    }

    public void documentDeleted(String pipeline) {
        record(deleted, "deleted", pipeline, 1);
    }

    /** Aggregate application of one sync pass. */
    public void applyPass(String pipeline, SyncPassRunner.PassOutcome outcome) {
        record(documents, "documents", pipeline, outcome.ingested() + outcome.replaced());
        record(replaced, "replaced", pipeline, outcome.replaced());
        record(skippedUnchanged, "skipped-unchanged", pipeline, outcome.skippedUnchanged());
        record(deleted, "deleted", pipeline, outcome.deleted());
        record(segments, "segments", pipeline, outcome.segmentsWritten());
        record(failures, "failures", pipeline, outcome.failed());
        record(deadLettered, "dead-lettered", pipeline, outcome.deadLettered());
        record(staleRetained, "stale-retained", pipeline, outcome.staleRetained());
    }

    private void record(ConcurrentMap<String, LongAdder> counters, String counter, String pipeline, long amount) {
        if (amount <= 0) {
            return;
        }
        counters.computeIfAbsent(pipeline, k -> new LongAdder()).add(amount);
        for (IngestMetricsListener listener : listeners) {
            listener.increment(pipeline, counter, amount);
        }
    }

    public Map<String, Long> documentsIngested() {
        return snapshot(documents);
    }

    public Map<String, Long> segmentsWritten() {
        return snapshot(segments);
    }

    public Map<String, Long> failures() {
        return snapshot(failures);
    }

    public Map<String, Long> replaced() {
        return snapshot(replaced);
    }

    public Map<String, Long> skippedUnchanged() {
        return snapshot(skippedUnchanged);
    }

    public Map<String, Long> deleted() {
        return snapshot(deleted);
    }

    public Map<String, Long> deadLettered() {
        return snapshot(deadLettered);
    }

    public Map<String, Long> staleRetained() {
        return snapshot(staleRetained);
    }

    private static Map<String, Long> snapshot(Map<String, LongAdder> counters) {
        Map<String, Long> out = new TreeMap<>();
        counters.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }
}
