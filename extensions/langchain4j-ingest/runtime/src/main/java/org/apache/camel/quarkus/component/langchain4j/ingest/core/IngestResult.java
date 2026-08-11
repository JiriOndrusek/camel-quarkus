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
package org.apache.camel.quarkus.component.langchain4j.ingest.core;

/**
 * Outcome of one ingestion operation.
 *
 * <p>
 * Outcomes: {@code ingested} (new document written), {@code replaced} (previous vectors
 * overwritten/removed), {@code skipped-unchanged} (change detection short-circuited — no fetch
 * of embeddings, no store write), {@code empty} (blank document, nothing written),
 * {@code deleted} (vectors removed), {@code suppressed-tombstone} (document was explicitly
 * deleted and stays deleted), {@code suppressed-pinned} (an API correction wins over the source
 * until unpinned).
 */
public record IngestResult(String pipeline, String documentId, int segmentsWritten, String outcome) {

    public static final String OUTCOME_INGESTED = "ingested";
    public static final String OUTCOME_REPLACED = "replaced";
    public static final String OUTCOME_SKIPPED_UNCHANGED = "skipped-unchanged";
    public static final String OUTCOME_EMPTY = "empty";
    public static final String OUTCOME_DELETED = "deleted";
    public static final String OUTCOME_SUPPRESSED_TOMBSTONE = "suppressed-tombstone";
    public static final String OUTCOME_SUPPRESSED_PINNED = "suppressed-pinned";
}
