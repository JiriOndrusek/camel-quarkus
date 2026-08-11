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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;

/**
 * Explicit knowledge-base corrections from application code — an admin screen, a legal hold, a
 * webhook. All operations are privileged and synchronous.
 *
 * <ul>
 * <li>{@link #delete} <b>sticks</b>: a tombstone survives every future source pass, so the
 * document stays deleted even while its source file still exists. Lift with
 * {@link #unsuppress}.</li>
 * <li>{@link #upsert} over a source-owned document <b>pins</b> it: the source stops updating it
 * until {@link #unpin} — a correction that silently reverts on the next poll would be worse
 * than none.</li>
 * </ul>
 */
@ApplicationScoped
public class IngestOperations {

    @Inject
    IngestPipelineRegistry registry;

    /** Runs the pipeline's own splitter and change detection; pins a source-owned document. */
    public IngestResult upsert(String pipeline, String documentId, String content) {
        return registry.require(pipeline).ingest(documentId, null, content, IngestService.Origin.API);
    }

    /** Removes the document's vectors and records a tombstone that survives future passes. */
    public IngestResult delete(String pipeline, String documentId) {
        return registry.require(pipeline).delete(documentId);
    }

    /** Lifts a tombstone; the next source pass re-ingests the document if it still exists. */
    public void unsuppress(String pipeline, String documentId) {
        registry.require(pipeline).unsuppress(documentId);
    }

    /** Freezes a document against source updates. */
    public void pin(String pipeline, String documentId) {
        registry.require(pipeline).pin(documentId);
    }

    /** Hands a pinned document back to its source. */
    public void unpin(String pipeline, String documentId) {
        registry.require(pipeline).unpin(documentId);
    }
}
