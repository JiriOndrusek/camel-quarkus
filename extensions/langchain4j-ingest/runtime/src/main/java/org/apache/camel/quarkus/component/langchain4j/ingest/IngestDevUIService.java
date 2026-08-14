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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.IngestService;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;

/**
 * Dev UI backend: the answer to "why doesn't my assistant know about file X" without any
 * vector-store spelunking — pipelines with their counters, and per-document tracker state.
 */
@ApplicationScoped
public class IngestDevUIService {

    @Inject
    IngestPipelineRegistry registry;

    @Inject
    IngestMetrics metrics;

    public List<Map<String, Object>> getPipelines() {
        Map<String, Boolean> readiness = registry.readiness();
        List<Map<String, Object>> pipelines = new ArrayList<>();
        for (Map.Entry<String, IngestService> entry : new TreeMap<>(registry.pipelines()).entrySet()) {
            String name = entry.getKey();
            Map<String, Object> pipeline = new LinkedHashMap<>();
            pipeline.put("name", name);
            pipeline.put("mode", entry.getValue().tracker() == null ? "append" : "sync");
            pipeline.put("ready", readiness.getOrDefault(name, true));
            pipeline.put("documents", metrics.documentsIngested().getOrDefault(name, 0L));
            pipeline.put("segments", metrics.segmentsWritten().getOrDefault(name, 0L));
            pipeline.put("replaced", metrics.replaced().getOrDefault(name, 0L));
            pipeline.put("skippedUnchanged", metrics.skippedUnchanged().getOrDefault(name, 0L));
            pipeline.put("deleted", metrics.deleted().getOrDefault(name, 0L));
            pipeline.put("deadLettered", metrics.deadLettered().getOrDefault(name, 0L));
            pipeline.put("failures", metrics.failures().getOrDefault(name, 0L));
            pipelines.add(pipeline);
        }
        return pipelines;
    }

    /** The tracker's view of a sync pipeline's documents; empty for append pipelines. */
    public List<Map<String, Object>> getDocuments(String pipeline) {
        IngestionTracker tracker = registry.require(pipeline).tracker();
        if (tracker == null) {
            return List.of();
        }
        List<Map<String, Object>> documents = new ArrayList<>();
        for (IngestionTracker.TrackerRow row : tracker.listDocuments(pipeline)) {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("documentId", row.documentId());
            document.put("status", row.status());
            document.put("segments", row.segmentCount());
            document.put("origin", row.origin());
            document.put("tombstone", row.tombstone());
            document.put("pinned", row.pinned());
            documents.add(document);
        }
        documents.sort((a, b) -> a.get("documentId").toString().compareTo(b.get("documentId").toString()));
        return documents;
    }
}
