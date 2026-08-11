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
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Build-time topology of ingestion pipelines: which pipelines exist, where documents come from
 * (source type), how they are split, and which store/model beans they write to. Locations and
 * credentials are runtime configuration, see {@link IngestRunTimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.camel.ai.ingest")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface IngestBuildTimeConfig {

    /**
     * Ingestion pipelines by name.
     */
    @WithParentName
    @ConfigDocMapKey("pipeline-name")
    Map<String, PipelineBuildTimeConfig> pipelines();

    interface PipelineBuildTimeConfig {

        /**
         * The document source.
         */
        SourceBuildTimeConfig source();

        /**
         * Ingestion mode. Only {@code append} is supported in this release: documents are only
         * ever added; a restart re-ingests the corpus. {@code sync} (update, replace,
         * restart-safety) arrives in a later release.
         */
        @WithDefault("append")
        String mode();

        /**
         * How documents are split into segments before embedding: {@code recursive} or
         * {@code none}.
         */
        @WithDefault("recursive")
        String splitter();

        /**
         * Maximum segment size in characters.
         */
        @WithDefault("500")
        int maxSegmentSize();

        /**
         * Maximum overlap between adjacent segments in characters.
         */
        @WithDefault("50")
        int maxOverlapSize();

        /**
         * Name of the {@code EmbeddingStore} bean this pipeline writes to. May be omitted when
         * exactly one store bean exists in the application.
         */
        Optional<String> embeddingStore();

        /**
         * Name of the {@code EmbeddingModel} bean used to embed segments. May be omitted when
         * exactly one model bean exists in the application.
         */
        Optional<String> embeddingModel();

        interface SourceBuildTimeConfig {

            /**
             * Source type. Only {@code file} is supported in this release; more source types
             * (s3, http, kafka) arrive in later releases.
             */
            String type();
        }
    }
}
