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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;

/**
 * The started pipelines by name. Populated by route setup; consumed by {@link IngestOperations}.
 */
@Singleton
public class IngestPipelineRegistry {

    private final ConcurrentMap<String, IngestService> pipelines = new ConcurrentHashMap<>();

    void register(String name, IngestService service) {
        pipelines.put(name, service);
    }

    public IngestService require(String name) {
        IngestService service = pipelines.get(name);
        if (service == null) {
            throw new IllegalArgumentException(
                    "Unknown ingestion pipeline '" + name + "'. Configured: "
                            + pipelines.keySet().stream().sorted().collect(Collectors.joining(", ")));
        }
        return service;
    }

    public Map<String, IngestService> pipelines() {
        return Map.copyOf(pipelines);
    }
}
