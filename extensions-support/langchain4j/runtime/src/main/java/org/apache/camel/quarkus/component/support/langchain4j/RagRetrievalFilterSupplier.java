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
package org.apache.camel.quarkus.component.support.langchain4j;

import dev.langchain4j.store.embedding.filter.Filter;

/**
 * The retrieval-side isolation hook: implemented as a CDI bean, consulted on <em>every</em>
 * retrieval performed by a produced {@link dev.langchain4j.rag.RetrievalAugmentor}. This is
 * what turns ingestion-side tenant metadata ({@code cq_tenant}) into an actual access control —
 * a reserved metadata key on its own isolates nothing.
 *
 * <p>
 * Typical implementation: derive the caller's tenant from the request context and return
 * {@code metadataKey("cq_tenant").isEqualTo(tenant)}.
 */
public interface RagRetrievalFilterSupplier {

    /**
     * @param  augmentorName the name of the augmentor performing the retrieval; {@code null}
     *                       for the auto-produced default augmentor
     * @return               the filter to apply, or {@code null} for unfiltered retrieval
     */
    Filter filter(String augmentorName);
}
