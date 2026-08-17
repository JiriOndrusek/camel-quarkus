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
 * Runtime configuration of ingestion pipelines: concrete locations and switches that may differ
 * per deployment. The pipeline topology is build-time, see {@link IngestBuildTimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.camel.ai.ingest")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface IngestRunTimeConfig {

    /**
     * Ingestion pipelines by name.
     */
    @WithParentName
    @ConfigDocMapKey("pipeline-name")
    Map<String, PipelineRunTimeConfig> pipelines();

    interface PipelineRunTimeConfig {

        /**
         * Whether this pipeline starts. Useful to switch ingestion off in dev mode.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The document source.
         */
        SourceRunTimeConfig source();

        /**
         * The sync tracker ({@code sync} mode).
         */
        TrackerRunTimeConfig tracker();

        /**
         * Reconciliation — deletion of documents that disappeared from the source
         * ({@code sync} mode).
         */
        ReconcileRunTimeConfig reconcile();

        /**
         * Readiness gating.
         */
        ReadinessRunTimeConfig readiness();

        /**
         * What happens when a pushed document fails to process ({@code kafka} and
         * {@code endpoint} sources, where a document is a Camel exchange): {@code skip} (log,
         * count, continue), {@code fail} (propagate to the consumer's error handling), or
         * {@code dead-letter} (route the failed exchange to {@code dead-letter-uri} via Camel's
         * Dead Letter Channel). Scan sources dead-letter through the tracker instead — a
         * document there is not an exchange.
         */
        @WithDefault("skip")
        String onFailure();

        /**
         * The Camel endpoint receiving failed exchanges when {@code on-failure=dead-letter},
         * for example a DLQ topic.
         */
        Optional<String> deadLetterUri();

        interface ReadinessRunTimeConfig {

            /**
             * Whether this pipeline gates application readiness until its first successful
             * synchronisation pass ({@code sync} mode). Disable for side-feature knowledge
             * bases that must not take the whole application out of rotation.
             */
            @WithDefault("true")
            boolean enabled();
        }

        interface TrackerRunTimeConfig {

            /**
             * Name of the datasource backing the sync tracker. When not set, the application's
             * default datasource is used. Dev Services provides one automatically in dev and
             * test mode when a JDBC driver is present and no datasource is configured.
             */
            Optional<String> datasource();
        }

        interface ReconcileRunTimeConfig {

            /**
             * A pass refusing to delete more than this fraction of the pipeline's source
             * documents without {@code allow-bulk-delete=true}: a mistyped directory or a
             * failed mount must be a refusal, not an emptied knowledge base.
             */
            @WithDefault("0.1")
            double bulkDeleteThreshold();

            /**
             * Explicit consent to bulk deletion (for intended corpus restructurings).
             */
            @WithDefault("false")
            boolean allowBulkDelete();
        }

        interface SourceRunTimeConfig {

            /**
             * The directory to ingest documents from ({@code file} source).
             */
            Optional<String> directory();

            /**
             * Whether subdirectories are ingested too ({@code file} source).
             */
            @WithDefault("true")
            boolean recursive();

            /**
             * Ant-style include pattern, for example {@code **&#47;*.txt} ({@code file} source).
             */
            Optional<String> include();

            /**
             * The document URL ({@code http} source). The URL is the document id; change
             * detection uses {@code ETag} / {@code Last-Modified} via a cheap HEAD request.
             */
            Optional<String> url();

            /**
             * The bucket to ingest from ({@code s3} source). The object key is the document id;
             * the ETag is the change fingerprint.
             */
            Optional<String> bucket();

            /**
             * AWS region ({@code s3} source).
             */
            Optional<String> region();

            /**
             * Only objects whose key starts with this prefix are ingested ({@code s3} source).
             */
            Optional<String> prefix();

            /**
             * Access key ({@code s3} source).
             */
            Optional<String> accessKey();

            /**
             * Secret key ({@code s3} source).
             */
            Optional<String> secretKey();

            /**
             * Endpoint override for S3-compatible stores such as MinIO ({@code s3} source).
             */
            Optional<String> endpointOverride();

            /**
             * The topic to consume ({@code kafka} source). The record key is the document id; a
             * record with a null payload (a compacted-topic tombstone) deletes the document.
             * Records without a key are skipped with a warning — replacement needs a stable id.
             */
            Optional<String> topic();

            /**
             * Broker list ({@code kafka} source). When not set, the component-level
             * {@code camel.component.kafka.brokers} configuration applies.
             */
            Optional<String> brokers();

            /**
             * Whether the consumer starts from the beginning of the topic on first start
             * ({@code kafka} source).
             */
            @WithDefault("true")
            boolean fromBeginning();

            /**
             * Interval between synchronisation passes in milliseconds ({@code sync} mode).
             */
            @WithDefault("5000")
            long pollInterval();
        }

        /**
         * Embedding call shaping.
         */
        EmbeddingRunTimeConfig embedding();

        interface EmbeddingRunTimeConfig {

            /**
             * Maximum number of segments embedded per model call.
             */
            @WithDefault("32")
            int batchSize();

            /**
             * Upper bound on embedding calls per minute. Unset means unthrottled.
             */
            Optional<Integer> requestsPerMinute();
        }
    }
}
