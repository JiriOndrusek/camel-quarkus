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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.util.URISupport;

/**
 * Dev UI backend: the pipelines as they actually run — one row per ingest route, with its
 * source, configuration highlights and live counters.
 */
@ApplicationScoped
public class IngestDevUIService {

    private static final String ROUTE_ID_PREFIX = "camel-quarkus-langchain4j-ingest-";

    @Inject
    CamelContext camelContext;

    @Inject
    IngestBuildTimeConfig buildTimeConfig;

    @Inject
    IngestRunTimeConfig runTimeConfig;

    @Inject
    IngestMetrics metrics;

    public List<Map<String, Object>> getPipelines() {
        List<Map<String, Object>> pipelines = new ArrayList<>();
        for (Route route : camelContext.getRoutes()) {
            if (!route.getRouteId().startsWith(ROUTE_ID_PREFIX)) {
                continue;
            }
            String name = route.getRouteId().substring(ROUTE_ID_PREFIX.length());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("source", URISupport.sanitizeUri(route.getEndpoint().getEndpointUri()));
            row.put("status", String.valueOf(
                    camelContext.getRouteController().getRouteStatus(route.getRouteId())));
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = buildTimeConfig.pipelines().get(name);
            row.put("parser", pipeline == null ? "" : pipeline.parser().orElse(""));
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);
            row.put("register", runtime == null ? "" : runtime.source().idempotentRepository().orElse(""));
            Map<String, Long> counters = metrics.counters(name);
            row.put("documents", counters.getOrDefault("documents", 0L));
            row.put("segments", counters.getOrDefault("segments", 0L));
            row.put("empty", counters.getOrDefault("empty", 0L));
            row.put("skipped", counters.getOrDefault("skipped", 0L));
            row.put("failures", counters.getOrDefault("failures", 0L));
            pipelines.add(row);
        }
        pipelines.sort(Comparator.comparing(row -> (String) row.get("name")));
        return pipelines;
    }
}
