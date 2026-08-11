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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestBuildTimeConfig;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestBuilderPipelines;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestMetrics;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestOperations;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipelineRegistry;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestRoutes;
import org.apache.camel.quarkus.component.langchain4j.ingest.Langchain4jIngestRecorder;
import org.apache.camel.quarkus.component.support.langchain4j.deployment.RagAugmentorCandidateBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

class Langchain4jIngestProcessor {

    private static final String FEATURE = "camel-langchain4j-ingest";

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of("file", "http", "s3", "kafka", "endpoint");
    /** Source types built on change detection and/or the deletion signal — they need the ledger. */
    private static final Set<String> SYNC_ONLY_SOURCE_TYPES = Set.of("http", "s3", "kafka");
    /** Curated source type → (Camel component, camel-quarkus extension artifact). */
    private static final Map<String, String[]> SOURCE_CONNECTORS = Map.of(
            "s3", new String[] { "aws2-s3", "camel-quarkus-aws2-s3" },
            "kafka", new String[] { "kafka", "camel-quarkus-kafka" });
    private static final Set<String> SUPPORTED_MODES = Set.of("append", "sync");
    private static final Set<String> SUPPORTED_SPLITTERS = Set.of("recursive", "none");
    private static final Set<String> SUPPORTED_WRITE_STRATEGIES = Set.of("upsert", "remove-then-add");
    private static final Set<String> SUPPORTED_ADOPT_MODES = Set.of("assume-empty", "wipe", "coexist");

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

    /**
     * A pipeline whose source needs a Camel connector that is not on the classpath stops the
     * build — with the command that fixes it, not a runtime ClassNotFoundException. Component
     * services are REGISTRY-destination, so they are looked up from the application archives
     * directly (they never appear among the DISCOVERY {@code CamelServiceBuildItem}s).
     */
    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void validateConnectorsPresent(IngestBuildTimeConfig config, ApplicationArchivesBuildItem applicationArchives) {
        PathFilter pathFilter = new PathFilter.Builder()
                .include("META-INF/services/org/apache/camel/component/*")
                .build();
        Set<String> components = CamelSupport.services(applicationArchives, pathFilter)
                .map(CamelServiceBuildItem::getName)
                .collect(Collectors.toSet());

        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            String[] connector = SOURCE_CONNECTORS.get(entry.getValue().source().type());
            if (connector != null && !components.contains(connector[0])) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + entry.getKey() + "' uses source type '"
                                + entry.getValue().source().type() + "' (Camel component '" + connector[0]
                                + "'), but that component is not on the classpath.\n"
                                + "Add it:  ./mvnw quarkus:add-extension -Dextensions=" + connector[1]);
            }
        }
    }

    /**
     * Discovers {@code @Ingest} builder methods: validated here (return type, no parameters,
     * unique names, no collision with configuration-declared pipelines), invoked reflectively
     * once at startup. Reflective invocation follows the {@code @Consume} pattern with its costs
     * accepted: native reflection registration, bean lookup, run-once side-effect-free methods.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void discoverBuilderPipelines(
            CombinedIndexBuildItem combinedIndex,
            IngestBuildTimeConfig config,
            Langchain4jIngestRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<RagAugmentorCandidateBuildItem> ragCandidates) {

        DotName ingestAnnotation = DotName.createSimple(Ingest.class.getName());
        DotName pipelineType = DotName.createSimple(IngestPipeline.class.getName());

        List<String> flatEntries = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> beanClasses = new HashSet<>();

        for (AnnotationInstance annotation : combinedIndex.getIndex().getAnnotations(ingestAnnotation)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo method = annotation.target().asMethod();
            String name = annotation.value().asString();
            String location = method.declaringClass().name() + "#" + method.name();

            if (name.isBlank()) {
                throw new ConfigurationException("@Ingest on " + location + " has a blank pipeline name");
            }
            if (!method.returnType().name().equals(pipelineType)) {
                throw new ConfigurationException(
                        "@Ingest method " + location + " must return " + IngestPipeline.class.getSimpleName());
            }
            if (!method.parameters().isEmpty()) {
                throw new ConfigurationException("@Ingest method " + location + " must take no parameters");
            }
            if (!names.add(name) || config.pipelines().containsKey(name)) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' is declared more than once (builder and/or "
                                + "configuration). Pipeline names must be unique.");
            }

            flatEntries.add(name);
            flatEntries.add(method.declaringClass().name().toString());
            flatEntries.add(method.name());
            beanClasses.add(method.declaringClass().name().toString());

            // builder pipelines join the RAG bridge like config pipelines do; their store/model
            // names are only known at runtime (the method body), so the augmentor falls back to
            // the @Default store and model beans
            ragCandidates.produce(new RagAugmentorCandidateBuildItem(name, null, null));
        }

        if (!beanClasses.isEmpty()) {
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(beanClasses.toArray(new String[0]))
                    .setUnremovable()
                    .build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(beanClasses.toArray(new String[0]))
                    .methods()
                    .build());
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(IngestBuilderPipelines.class)
                .scope(Singleton.class)
                .unremovable()
                .runtimeValue(recorder.createBuilderPipelines(flatEntries))
                .done());
    }

    /** Micrometer exposition of the ingestion counters, active only when Micrometer is present. */
    @BuildStep
    void micrometerMetrics(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (capabilities.isPresent(Capability.METRICS)) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(
                    "org.apache.camel.quarkus.component.langchain4j.ingest.IngestMicrometerListener"));
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

            if (SYNC_ONLY_SOURCE_TYPES.contains(sourceType) && !"sync".equals(mode)) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' combines source type '" + sourceType + "' with "
                                + "mode=append. This source is built on change detection and/or the deletion "
                                + "signal and needs mode=sync (and a datasource for the ledger).");
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

            if (!SUPPORTED_ADOPT_MODES.contains(pipeline.adopt())) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + name + "' has unknown adopt mode '" + pipeline.adopt()
                                + "'. Supported: " + SUPPORTED_ADOPT_MODES);
            }
        }

        // wipe is store-wide (removeAll()), so it must not fire on a store other pipelines share
        Map<String, List<String>> pipelinesByStore = new java.util.HashMap<>();
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            pipelinesByStore
                    .computeIfAbsent(entry.getValue().embeddingStore().orElse("<default>"), k -> new ArrayList<>())
                    .add(entry.getKey());
        }
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            List<String> sharing = pipelinesByStore.get(entry.getValue().embeddingStore().orElse("<default>"));
            if ("wipe".equals(entry.getValue().adopt()) && sharing.size() > 1) {
                throw new ConfigurationException(
                        "Ingestion pipeline '" + entry.getKey() + "' has adopt=wipe, but " + sharing.size()
                                + " pipelines share its embedding store (" + String.join(", ", sharing)
                                + ") and removeAll() is store-wide — one pipeline's fresh start must not "
                                + "destroy its siblings' corpora. Give the wiping pipeline its own store.");
            }
        }
    }
}
