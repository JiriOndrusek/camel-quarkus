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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.support.langchain4j.RagRetrievalFilterSupplier;

/**
 * The retrieval-side isolation hook under test: when a tenant is set (via REST), every
 * retrieval through the produced augmentor is filtered to that tenant's documents.
 */
@Singleton
public class TestTenantFilterSupplier implements RagRetrievalFilterSupplier {

    static volatile String tenant;

    @Override
    public Filter filter(String augmentorName) {
        return tenant == null ? null : MetadataFilterBuilder.metadataKey("cq_tenant").isEqualTo(tenant);
    }
}
