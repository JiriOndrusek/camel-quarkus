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

import jakarta.inject.Singleton;

/**
 * The three core ingestion counters, per pipeline: documents ingested, segments written,
 * failures. "It fails loudly" is a first-release promise, so these exist from the start;
 * exposition through Micrometer follows in a later release.
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

    public void documentIngested(String pipeline, int segmentsWritten) {
        documents.computeIfAbsent(pipeline, k -> new LongAdder()).increment();
        segments.computeIfAbsent(pipeline, k -> new LongAdder()).add(segmentsWritten);
    }

    public void documentReplaced(String pipeline, int segmentsWritten) {
        documentIngested(pipeline, segmentsWritten);
        replaced.computeIfAbsent(pipeline, k -> new LongAdder()).increment();
    }

    public void documentSkippedUnchanged(String pipeline) {
        skippedUnchanged.computeIfAbsent(pipeline, k -> new LongAdder()).increment();
    }

    public void failure(String pipeline) {
        failures.computeIfAbsent(pipeline, k -> new LongAdder()).increment();
    }

    public void documentDeleted(String pipeline) {
        deleted.computeIfAbsent(pipeline, k -> new LongAdder()).increment();
    }

    /** Aggregate application of one sync pass. */
    public void applyPass(String pipeline,
            org.apache.camel.quarkus.component.langchain4j.ingest.core.SyncPassRunner.PassOutcome outcome) {
        add(documents, pipeline, outcome.ingested() + outcome.replaced());
        add(replaced, pipeline, outcome.replaced());
        add(skippedUnchanged, pipeline, outcome.skippedUnchanged());
        add(deleted, pipeline, outcome.deleted());
        add(segments, pipeline, outcome.segmentsWritten());
        add(failures, pipeline, outcome.failed());
        add(deadLettered, pipeline, outcome.deadLettered());
        add(staleRetained, pipeline, outcome.staleRetained());
    }

    private static void add(ConcurrentMap<String, LongAdder> counters, String pipeline, int amount) {
        if (amount > 0) {
            counters.computeIfAbsent(pipeline, k -> new LongAdder()).add(amount);
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
