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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Reports not-ready until every gating sync pipeline completed its first successful pass —
 * a pod serving RAG answers from a half-populated knowledge base answers <em>worse</em>, not
 * failing, so readiness is the only signal there is. The initial pass never blocks boot (a
 * crash-looping startup probe would be worse); readiness gates load-balanced traffic only.
 * Registered only when SmallRye Health is present; per-pipeline opt-out via
 * {@code quarkus.camel.ai.ingest.<name>.readiness.enabled=false}.
 */
@Readiness
@ApplicationScoped
public class IngestReadinessCheck implements HealthCheck {

    @Inject
    IngestPipelineRegistry registry;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("camel-langchain4j-ingest");
        for (Map.Entry<String, Boolean> entry : registry.readiness().entrySet()) {
            builder.withData(entry.getKey(), entry.getValue() ? "ready" : "awaiting first successful pass");
        }
        return builder.status(registry.allReady()).build();
    }
}
