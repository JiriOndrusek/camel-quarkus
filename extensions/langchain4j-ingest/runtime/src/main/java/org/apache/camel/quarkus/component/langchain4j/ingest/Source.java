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

import java.util.Optional;

import org.apache.camel.builder.EndpointConsumerBuilder;

/**
 * Where a builder-declared pipeline's documents come from — the type-safe twin of the
 * {@code quarkus.camel.ai.ingest.<name>.source.*} configuration. Every option here has a
 * properties equivalent (the parity rule).
 */
public final class Source {

    private final String type;
    private final String uri;

    // file
    private String directory;
    private boolean recursive = true;
    private String include;
    // http
    private String url;
    // s3
    private String bucket;
    private String region;
    private String prefix;
    private String accessKey;
    private String secretKey;
    private String endpointOverride;
    // kafka
    private String topic;
    private String brokers;
    private boolean fromBeginning = true;
    // scan sources
    private long pollInterval = 5000;

    private Source(String type, String uri) {
        this.type = type;
        this.uri = uri;
    }

    /** A directory: relative file paths are the document ids, size+mtime the fingerprints. */
    public static Source file(String directory) {
        Source source = new Source("file", null);
        source.directory = directory;
        return source;
    }

    /** One URL, one document: ETag / Last-Modified drive change detection, 404 deletes. */
    public static Source http(String url) {
        Source source = new Source("http", null);
        source.url = url;
        return source;
    }

    /** A bucket: object keys are the document ids, ETags the fingerprints. */
    public static Source s3(String bucket) {
        Source source = new Source("s3", null);
        source.bucket = bucket;
        return source;
    }

    /** A topic: record keys are the document ids, null-payload tombstones delete. */
    public static Source kafka(String topic) {
        Source source = new Source("kafka", null);
        source.topic = topic;
        return source;
    }

    /** Any Camel consumer URI — each message needs the {@code CamelAiIngestDocumentId} header. */
    public static Source endpoint(String uri) {
        return new Source("endpoint", uri);
    }

    /**
     * The Camel Endpoint DSL form: one static import buys typed autocompletion over a
     * connector's full option set. Note the sharp edge: the DSL factories all ship in one
     * artifact, so this compiles even when the connector extension is absent — the missing
     * component is reported at startup, not at compile time.
     */
    public static Source endpoint(EndpointConsumerBuilder builder) {
        return new Source("endpoint", builder.getRawUri());
    }

    public Source recursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    /** Ant-style include pattern, for example {@code **}{@code /*.txt}. */
    public Source include(String include) {
        this.include = include;
        return this;
    }

    public Source region(String region) {
        this.region = region;
        return this;
    }

    public Source prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public Source accessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public Source secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    /** For S3-compatible stores such as MinIO. */
    public Source endpointOverride(String endpointOverride) {
        this.endpointOverride = endpointOverride;
        return this;
    }

    public Source brokers(String brokers) {
        this.brokers = brokers;
        return this;
    }

    public Source fromBeginning(boolean fromBeginning) {
        this.fromBeginning = fromBeginning;
        return this;
    }

    /** Interval between synchronisation passes in milliseconds. */
    public Source pollInterval(long pollIntervalMillis) {
        this.pollInterval = pollIntervalMillis;
        return this;
    }

    String type() {
        return type;
    }

    String uri() {
        return uri;
    }

    /** The runtime-config view of this source, so builder pipelines reuse the config paths. */
    IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig asRunTimeConfig() {
        return new IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig() {
            @Override
            public Optional<String> directory() {
                return Optional.ofNullable(directory);
            }

            @Override
            public boolean recursive() {
                return recursive;
            }

            @Override
            public Optional<String> include() {
                return Optional.ofNullable(include);
            }

            @Override
            public Optional<String> url() {
                return Optional.ofNullable(url);
            }

            @Override
            public Optional<String> bucket() {
                return Optional.ofNullable(bucket);
            }

            @Override
            public Optional<String> region() {
                return Optional.ofNullable(region);
            }

            @Override
            public Optional<String> prefix() {
                return Optional.ofNullable(prefix);
            }

            @Override
            public Optional<String> accessKey() {
                return Optional.ofNullable(accessKey);
            }

            @Override
            public Optional<String> secretKey() {
                return Optional.ofNullable(secretKey);
            }

            @Override
            public Optional<String> endpointOverride() {
                return Optional.ofNullable(endpointOverride);
            }

            @Override
            public Optional<String> topic() {
                return Optional.ofNullable(topic);
            }

            @Override
            public Optional<String> brokers() {
                return Optional.ofNullable(brokers);
            }

            @Override
            public boolean fromBeginning() {
                return fromBeginning;
            }

            @Override
            public long pollInterval() {
                return pollInterval;
            }
        };
    }
}
