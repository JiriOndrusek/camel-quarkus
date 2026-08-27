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

import java.util.List;
import java.util.Map;

import org.apache.camel.ServiceStatus;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

/**
 * A Camel readiness check naming each ingestion pipeline: ready when every pipeline's route is
 * started. Being a Camel health check it is exposed through whatever the application already
 * uses — with {@code camel-quarkus-microprofile-health} it appears under {@code /q/health/ready}
 * — and it costs nothing without one. The synchronising engine will enrich the semantics behind
 * the same check name (ready only after a pipeline's first successful pass).
 */
public class IngestReadinessCheck extends AbstractHealthCheck {

    private final List<String> pipelines;

    public IngestReadinessCheck(List<String> pipelines) {
        super("camel-quarkus-langchain4j-ingest");
        this.pipelines = pipelines;
    }

    @Override
    public boolean isLiveness() {
        return false;
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        boolean allStarted = true;
        for (String pipeline : pipelines) {
            ServiceStatus status = getCamelContext().getRouteController()
                    .getRouteStatus("camel-quarkus-langchain4j-ingest-" + pipeline);
            boolean started = status != null && status.isStarted();
            builder.detail(pipeline, started ? "started" : String.valueOf(status).toLowerCase(java.util.Locale.ROOT));
            allStarted &= started;
        }
        if (allStarted) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
