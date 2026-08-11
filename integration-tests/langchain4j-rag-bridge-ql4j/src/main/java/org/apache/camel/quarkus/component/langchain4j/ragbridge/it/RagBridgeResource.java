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

import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/rag-bridge")
public class RagBridgeResource {

    @Inject
    Instance<RetrievalAugmentor> retrievalAugmentorInstance;

    @Inject
    @Named("products")
    Instance<RetrievalAugmentor> namedAugmentorInstance;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Inject
    RagAiService aiService;

    @GET
    @Path("/augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isAugmentorPresent() {
        return retrievalAugmentorInstance.isResolvable();
    }

    @GET
    @Path("/named-augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNamedAugmentorPresent() {
        return namedAugmentorInstance.isResolvable();
    }

    @POST
    @Path("/ingest")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingest(String text) {
        producerTemplate.sendBody("direct:ingest", text);
        return "ingested";
    }

    @POST
    @Path("/ingest-products")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingestProducts(String text) {
        producerTemplate.sendBody("direct:ingest-products", text);
        return "ingested";
    }

    @GET
    @Path("/registry/embedding-store/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isEmbeddingStoreInRegistry(@PathParam("name") String name) {
        EmbeddingStore<?> store = camelContext.getRegistry().lookupByNameAndType(name, EmbeddingStore.class);
        return store != null;
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String question) {
        return aiService.chat(question);
    }

    // --- retrieval filter hook -----------------------------------------------------------

    @Inject
    @Named("defaultStore")
    EmbeddingStore<dev.langchain4j.data.segment.TextSegment> defaultStore;

    @Inject
    dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    /** Seeds the default store with a tenant-tagged segment, like the ingest extension does. */
    @POST
    @Path("/seed/{tenant}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String seed(@jakarta.ws.rs.PathParam("tenant") String tenant, String text) {
        dev.langchain4j.data.segment.TextSegment segment = dev.langchain4j.data.segment.TextSegment.from(text,
                dev.langchain4j.data.document.Metadata.from(java.util.Map.of("cq_tenant", tenant)));
        defaultStore.add(embeddingModel.embed(segment).content(), segment);
        return "seeded";
    }

    @POST
    @Path("/tenant-filter/{tenant}")
    @Produces(MediaType.TEXT_PLAIN)
    public String setTenantFilter(@jakarta.ws.rs.PathParam("tenant") String tenant) {
        TestTenantFilterSupplier.tenant = "none".equals(tenant) ? null : tenant;
        return "ok";
    }

    /** Runs the produced default augmentor directly, returning the retrieved segment texts. */
    @POST
    @Path("/augment")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<String> augment(String question) {
        dev.langchain4j.data.message.UserMessage userMessage = dev.langchain4j.data.message.UserMessage
                .from(question);
        dev.langchain4j.rag.AugmentationResult result = retrievalAugmentorInstance.get()
                .augment(new dev.langchain4j.rag.AugmentationRequest(userMessage,
                        dev.langchain4j.rag.query.Metadata.from(userMessage, null, null)));
        return result.contents().stream().map(content -> content.textSegment().text()).toList();
    }
}
