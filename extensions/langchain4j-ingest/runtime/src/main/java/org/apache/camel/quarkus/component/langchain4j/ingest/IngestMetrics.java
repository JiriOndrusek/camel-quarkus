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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;

/**
 * The per-pipeline ingestion counters: kept in-process (the Dev UI reads them) and forwarded to
 * every {@link IngestMetricsListener} bean — with Micrometer present the increments also appear
 * as {@code cq.ingest.*} meters.
 */
@Singleton
public class IngestMetrics {

    private final ConcurrentMap<String, ConcurrentMap<String, LongAdder>> counters = new ConcurrentHashMap<>();

    @Inject
    @Any
    Instance<IngestMetricsListener> listeners;

    /** Counts one completed ingestion by its outcome; INGESTED also counts the segments. */
    public void record(IngestResult result) {
        switch (result.outcome()) {
        case INGESTED -> {
            increment(result.pipeline(), "documents", 1);
            increment(result.pipeline(), "segments", result.segmentsWritten());
        }
        case EMPTY -> increment(result.pipeline(), "empty", 1);
        case SKIPPED -> increment(result.pipeline(), "skipped", 1);
        }
    }

    /** Counts an ingestion that threw. */
    public void failure(String pipeline) {
        increment(pipeline, "failures", 1);
    }

    /** A snapshot of one pipeline's counters; a counter never incremented is absent. */
    public Map<String, Long> counters(String pipeline) {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        counters.getOrDefault(pipeline, new ConcurrentHashMap<>())
                .forEach((counter, adder) -> snapshot.put(counter, adder.sum()));
        return snapshot;
    }

    private void increment(String pipeline, String counter, long amount) {
        counters.computeIfAbsent(pipeline, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(counter, key -> new LongAdder())
                .add(amount);
        for (IngestMetricsListener listener : listeners) {
            listener.increment(pipeline, counter, amount);
        }
    }
}
