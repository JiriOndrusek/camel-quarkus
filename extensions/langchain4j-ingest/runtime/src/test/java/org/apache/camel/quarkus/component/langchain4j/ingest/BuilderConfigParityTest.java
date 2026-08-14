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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.smallrye.config.WithName;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parity rule: every configuration knob has a builder counterpart and vice versa. The map
 * below is the contract — adding a configuration property without extending the builder (or
 * consciously recording the exemption here) fails this test, and so does removing either side.
 */
class BuilderConfigParityTest {

    /** property path → builder counterpart ("Class.method"), or a recorded exemption ("-"). */
    static final Map<String, String> PARITY = Map.ofEntries(
            entry("source.type", "Source.file|http|s3|kafka|endpoint"),
            entry("source.uri", "Source.endpoint"),
            entry("source.directory", "Source.file"),
            entry("source.recursive", "Source.recursive"),
            entry("source.include", "Source.include"),
            entry("source.url", "Source.http"),
            entry("source.bucket", "Source.s3"),
            entry("source.region", "Source.region"),
            entry("source.prefix", "Source.prefix"),
            entry("source.access-key", "Source.accessKey"),
            entry("source.secret-key", "Source.secretKey"),
            entry("source.endpoint-override", "Source.endpointOverride"),
            entry("source.topic", "Source.kafka"),
            entry("source.brokers", "Source.brokers"),
            entry("source.from-beginning", "Source.fromBeginning"),
            entry("source.poll-interval", "Source.pollInterval"),
            entry("mode", "IngestPipeline.sync"),
            entry("write-strategy", "IngestPipeline.writeStrategy"),
            entry("splitter", "IngestPipeline.splitter"),
            entry("max-segment-size", "IngestPipeline.splitter"),
            entry("max-overlap-size", "IngestPipeline.splitter"),
            entry("embedding-store", "IngestPipeline.embeddingStore"),
            entry("embedding-model", "IngestPipeline.embeddingModel"),
            entry("embedding-model-id", "IngestPipeline.embeddingModelId"),
            entry("adopt", "IngestPipeline.adopt"),
            // enabled is a deployment-side switch; builder pipelines are disabled with the
            // same runtime property quarkus.camel.ai.ingest.<name>.enabled=false
            entry("enabled", "-"),
            entry("tracker.datasource", "IngestPipeline.trackerDatasource"),
            entry("reconcile.bulk-delete-threshold", "IngestPipeline.bulkDeleteThreshold"),
            entry("reconcile.allow-bulk-delete", "IngestPipeline.allowBulkDelete"),
            entry("readiness.enabled", "IngestPipeline.readinessEnabled"),
            entry("leader-only", "IngestPipeline.leaderOnly"),
            entry("on-failure", "IngestPipeline.onFailure"),
            entry("dead-letter-uri", "IngestPipeline.deadLetterUri"),
            entry("embedding.batch-size", "IngestPipeline.embeddingBatchSize"),
            entry("embedding.requests-per-minute", "IngestPipeline.embeddingRequestsPerMinute"));

    @Test
    void everyConfigurationPropertyHasABuilderCounterpart() {
        Set<String> properties = new TreeSet<>();
        collect(IngestBuildTimeConfig.PipelineBuildTimeConfig.class, "", properties);
        collect(IngestRunTimeConfig.PipelineRunTimeConfig.class, "", properties);

        assertEquals(new TreeSet<>(PARITY.keySet()), properties,
                "configuration surface and the PARITY contract diverged — extend the builder "
                        + "(or record an exemption) for every added property");
    }

    @Test
    void everyDeclaredBuilderCounterpartExists() {
        for (Map.Entry<String, String> mapping : PARITY.entrySet()) {
            String counterpart = mapping.getValue();
            if ("-".equals(counterpart)) {
                continue;
            }
            String className = counterpart.substring(0, counterpart.indexOf('.'));
            String methods = counterpart.substring(counterpart.indexOf('.') + 1);
            Class<?> builderClass = "Source".equals(className) ? Source.class : IngestPipeline.class;
            for (String methodName : methods.split("\\|")) {
                assertTrue(Arrays.stream(builderClass.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals(methodName)),
                        "builder method " + className + "." + methodName + " (counterpart of '"
                                + mapping.getKey() + "') does not exist");
            }
        }
    }

    private static void collect(Class<?> configInterface, String prefix, Set<String> out) {
        for (Method method : configInterface.getDeclaredMethods()) {
            String name = method.isAnnotationPresent(WithName.class)
                    ? method.getAnnotation(WithName.class).value()
                    : kebab(method.getName());
            if (method.getReturnType().isInterface()
                    && method.getReturnType().getEnclosingClass() != null
                    && method.getReturnType().getName().contains("Config")) {
                collect(method.getReturnType(), prefix + name + ".", out);
            } else {
                out.add(prefix + name);
            }
        }
    }

    private static String kebab(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
