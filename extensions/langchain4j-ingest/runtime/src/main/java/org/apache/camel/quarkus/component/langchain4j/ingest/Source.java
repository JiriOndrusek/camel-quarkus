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

import org.apache.camel.builder.EndpointConsumerBuilder;

/**
 * Where a builder-declared pipeline's documents come from. This release ships the escape hatch;
 * curated factories ({@code Source.s3(...)}, ...) follow.
 */
public final class Source {

    private final String uri;

    private Source(String uri) {
        this.uri = uri;
    }

    /** Any Camel consumer URI — each message needs the {@code CamelAiIngestDocumentId} header. */
    public static Source endpoint(String uri) {
        return new Source(uri);
    }

    /**
     * The Camel Endpoint DSL form: one static import buys typed autocompletion over a
     * connector's full option set. Note the sharp edge: the DSL factories all ship in one
     * artifact, so this compiles even when the connector extension is absent — the missing
     * component is reported at startup, not at compile time.
     */
    public static Source endpoint(EndpointConsumerBuilder builder) {
        return new Source(builder.getRawUri());
    }

    String uri() {
        return uri;
    }
}
