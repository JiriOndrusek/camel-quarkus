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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Publishes the ingestion counters as Micrometer meters — {@code cq.ingest.<counter>}, tagged
 * with the pipeline name. Registered as a bean only when the Micrometer capability is present,
 * which is why Micrometer stays an optional dependency of this extension.
 */
@Singleton
public class MicrometerIngestMetricsListener implements IngestMetricsListener {

    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    @Inject
    MeterRegistry registry;

    @Override
    public void increment(String pipeline, String counter, long amount) {
        counters.computeIfAbsent(counter + '|' + pipeline,
                key -> Counter.builder("cq.ingest." + counter)
                        .tag("pipeline", pipeline)
                        .register(registry))
                .increment(amount);
    }
}
