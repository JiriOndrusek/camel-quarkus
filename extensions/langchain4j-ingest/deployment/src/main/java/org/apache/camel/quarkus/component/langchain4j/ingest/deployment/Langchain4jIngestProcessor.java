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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestBuildTimeConfig;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestMetrics;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestOperations;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipelineRegistry;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestRoutes;
import org.apache.camel.quarkus.component.support.langchain4j.deployment.RagAugmentorCandidateBuildItem;

class Langchain4jIngestProcessor {

    private static final String FEATURE = "camel-langchain4j-ingest";

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of("file", "http", "endpoint");
    private static final Set<String> SUPPORTED_MODES = Set.of("append", "sync");
    private static final Set<String> SUPPORTED_SPLITTERS = Set.of("recursive", "none");
    private static final Set<String> SUPPORTED_WRITE_STRATEGIES = Set.of("upsert", "remove-then-add");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * One augmentor candidate per pipeline: declaring an ingestion pipeline is enough for a
     * matching {@code @Named} RetrievalAugmentor to exist when Quarkus LangChain4j is present.
     * Explicit {@code quarkus.camel.langchain4j.rag.augmentors.<name>} config wins on collision,
     * and the designated-default rules of the RAG bridge apply (two pipelines with no default
     * marked fail the build instead of silently disabling RAG).
     */
    @BuildStep
    void ragAugmentorCandidates(IngestBuildTimeConfig config,
            BuildProducer<RagAugmentorCandidateBuildItem> candidates) {
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = entry.getValue();
            candidates.produce(new RagAugmentorCandidateBuildItem(
                    entry.getKey(),
                    pipeline.embeddingStore().orElse(null),
                    pipeline.embeddingModel().orElse(null)));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(IngestMetrics.class, IngestRoutes.class, IngestPipelineRegistry.class,
                        IngestOperations.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    void readinessCheck(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(
                    "org.apache.camel.quarkus.component.langchain4j.ingest.IngestReadinessCheck"));
        }
    }

    /**
     * Everything that can be decided from build-time configuration fails here, at build time,
     * with the fix in the message — never silently at runtime.
     */
    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void validatePipelines(IngestBuildTimeConfig config) {
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            String name = entry.getKey();
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = entry.getValue();

            String mode = pipeline.mode();
            if (!SUPPORTED_MODES.contains(mode)) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has unknown mode '" + mode + "'. Supported: "
                                + SUPPORTED_MODES);
            }

            if (!SUPPORTED_WRITE_STRATEGIES.contains(pipeline.writeStrategy())) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has unknown write-strategy '" + pipeline.writeStrategy()
                                + "'. Supported: " + SUPPORTED_WRITE_STRATEGIES);
            }

            String sourceType = pipeline.source().type();
            if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has source type '" + sourceType + "'. This preview "
                                + "supports " + SUPPORTED_SOURCE_TYPES + "; more curated types (s3, kafka) "
                                + "arrive in later releases — meanwhile any Camel consumer works via "
                                + "source.type=endpoint.");
            }

            if ("endpoint".equals(sourceType) && pipeline.source().uri().isEmpty()) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has source type 'endpoint' but no uri. Set "
                                + "quarkus.camel.ai.ingest." + name + ".source.uri (build-time by design: a "
                                + "runtime-overridable consumer URI would be arbitrary component invocation).");
            }

            if ("http".equals(sourceType) && !"sync".equals(mode)) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' combines source type 'http' with mode=append. The "
                                + "http source is built on change detection and needs mode=sync (and a "
                                + "datasource for the ledger).");
            }

            if (!SUPPORTED_SPLITTERS.contains(pipeline.splitter())) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has unknown splitter '" + pipeline.splitter()
                                + "'. Supported: " + SUPPORTED_SPLITTERS);
            }

            if (pipeline.maxSegmentSize() <= 0 || pipeline.maxOverlapSize() < 0
                    || pipeline.maxOverlapSize() >= pipeline.maxSegmentSize()) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "': max-segment-size must be positive and "
                                + "max-overlap-size must be smaller than max-segment-size (got "
                                + pipeline.maxSegmentSize() + " / " + pipeline.maxOverlapSize() + ")");
            }
        }
    }
}
