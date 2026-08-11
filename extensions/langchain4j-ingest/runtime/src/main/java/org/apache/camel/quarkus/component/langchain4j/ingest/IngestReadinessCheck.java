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

import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

/**
 * A Camel readiness check: not ready until every gating sync pipeline completed its first
 * successful pass — a pod serving RAG answers from a half-populated knowledge base answers
 * <em>worse</em>, not failing, so readiness is the only signal there is. The initial pass never
 * blocks boot; readiness gates load-balanced traffic only.
 *
 * <p>
 * Being a Camel health check, it is exposed through whatever the application already uses —
 * with {@code camel-quarkus-microprofile-health} it appears under {@code /q/health/ready}.
 * Per-pipeline opt-out via {@code quarkus.camel.ai.ingest.<name>.readiness.enabled=false}.
 */
public class IngestReadinessCheck extends AbstractHealthCheck {

    private final IngestPipelineRegistry registry;

    public IngestReadinessCheck(IngestPipelineRegistry registry) {
        super("camel-langchain4j-ingest");
        this.registry = registry;
    }

    @Override
    public boolean isLiveness() {
        return false;
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        boolean allReady = true;
        for (Map.Entry<String, Boolean> entry : registry.readiness().entrySet()) {
            builder.detail(entry.getKey(), entry.getValue() ? "ready" : "awaiting first successful pass");
            allReady &= entry.getValue();
        }
        if (allReady) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
