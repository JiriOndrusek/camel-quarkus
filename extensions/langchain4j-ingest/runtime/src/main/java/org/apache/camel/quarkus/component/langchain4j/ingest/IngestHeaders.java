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

/**
 * Headers of the {@code direct:ingest-<pipeline>} ingress.
 */
public final class IngestHeaders {

    /**
     * Required: the stable document id. Update and delete semantics are built on it, so a
     * generated fallback would silently break replacement — an absent id is an error.
     */
    public static final String DOCUMENT_ID = "CamelAiIngestDocumentId";

    /**
     * Optional: a cheap change fingerprint (size+mtime, an ETag, an offset). When present and
     * unchanged, a {@code sync} pipeline skips the document without splitting or embedding.
     * Absent means "always run change detection on content".
     */
    public static final String FINGERPRINT = "CamelAiIngestFingerprint";

    /**
     * Optional: the tenant this document belongs to, written as segment metadata
     * ({@code cq_tenant}) for retrieval-side isolation via {@code RagRetrievalFilterSupplier}.
     */
    public static final String TENANT = "CamelAiIngestTenant";

    private IngestHeaders() {
    }
}
