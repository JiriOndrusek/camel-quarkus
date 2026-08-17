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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestHeaders;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestMetrics;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.IngestResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/langchain4j-ingest")
public class IngestResource {

    @Inject
    @Named("products-store")
    EmbeddingStore<TextSegment> productsStore;

    @Inject
    @Named("manuals-store")
    EmbeddingStore<TextSegment> manualsStore;

    @Inject
    @Named("webdoc-store")
    EmbeddingStore<TextSegment> webdocStore;

    @Inject
    @Named("custom-store")
    EmbeddingStore<TextSegment> customStore;

    @Inject
    @Named("s3-store")
    EmbeddingStore<TextSegment> s3Store;

    @Inject
    @Named("events-store")
    EmbeddingStore<TextSegment> eventsStore;

    @Inject
    @Named("test-model")
    EmbeddingModel model;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    IngestMetrics metrics;

    @Inject
    org.apache.camel.quarkus.component.langchain4j.ingest.IngestOperations operations;

    @ConfigProperty(name = "ingest.test.directory")
    String directory;

    @ConfigProperty(name = "ingest.test.sync-directory")
    String syncDirectory;

    /** Writes a document into a watched directory — app-side, so native mode shares the path. */
    @POST
    @Path("/file/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void writeFile(@PathParam("name") String name, @QueryParam("pipeline") String pipeline, String content)
            throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(directoryOf(pipeline));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), content);
    }

    private String directoryOf(String pipeline) {
        return switch (pipeline == null ? "products" : pipeline) {
        case "manuals" -> syncDirectory;
        default -> directory;
        };
    }

    @DELETE
    @Path("/file/{name}")
    public void deleteFile(@PathParam("name") String name, @QueryParam("pipeline") String pipeline)
            throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("manuals".equals(pipeline) ? syncDirectory : directory);
        Files.deleteIfExists(dir.resolve(name));
    }

    @POST
    @Path("/ops/{operation}/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> operation(@PathParam("operation") String operation,
            @PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId, String content) {
        switch (operation) {
        case "upsert" -> {
            var result = operations.upsert(pipeline, documentId, content);
            return Map.of("outcome", result.outcome().label(), "segmentsWritten", result.segmentsWritten());
        }
        case "delete" -> {
            var result = operations.delete(pipeline, documentId);
            return Map.of("outcome", result.outcome().label());
        }
        case "unsuppress" -> operations.unsuppress(pipeline, documentId);
        case "pin" -> operations.pin(pipeline, documentId);
        case "unpin" -> operations.unpin(pipeline, documentId);
        default -> throw new IllegalArgumentException("Unknown operation " + operation);
        }
        return Map.of("outcome", "ok");
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> search(@QueryParam("q") String query, @QueryParam("max") Integer max,
            @QueryParam("store") String storeName) {
        EmbeddingStore<TextSegment> store = switch (storeName == null ? "products" : storeName) {
        case "manuals" -> manualsStore;
        case "webdoc" -> webdocStore;
        case "custom" -> customStore;
        case "s3" -> s3Store;
        case "events" -> eventsStore;
        default -> productsStore;
        };
        var result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(model.embed(query).content())
                .maxResults(max == null ? 5 : max)
                .minScore(0.0)
                .build());
        return result.matches().stream().map(m -> m.embedded().text()).toList();
    }

    @POST
    @Path("/ingress/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ingress(@PathParam("documentId") String documentId, String content) {
        IngestResult result = producerTemplate.requestBodyAndHeader(
                "direct:ingest-products", content, IngestHeaders.DOCUMENT_ID, documentId, IngestResult.class);
        return Response.ok(Map.of(
                "pipeline", result.pipeline(),
                "documentId", result.documentId(),
                "segmentsWritten", result.segmentsWritten(),
                "outcome", result.outcome().label())).build();
    }

    /** Ingress call without the required document id header — must fail with a clear error. */
    @POST
    @Path("/ingress-without-id")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response ingressWithoutId(String content) {
        try {
            producerTemplate.requestBody("direct:ingest-products", content);
            return Response.ok().build();
        } catch (CamelExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException iae) {
                return Response.status(400).entity(iae.getMessage()).build();
            }
            throw e;
        }
    }

    // --- the http-source pipeline's document, served by the app itself -----------------------

    static volatile String httpDocContent = "The web manual mentions the FALCON-9000 torque wrench.";
    static volatile boolean httpDocGone = false;

    @GET
    @Path("/http-doc")
    @Produces(MediaType.TEXT_PLAIN)
    public Response httpDoc() {
        if (httpDocGone) {
            return Response.status(404).build();
        }
        return Response.ok(httpDocContent)
                .header("ETag", "\"" + Integer.toHexString(httpDocContent.hashCode()) + "\"")
                .build();
    }

    @POST
    @Path("/http-doc")
    @Consumes(MediaType.TEXT_PLAIN)
    public void setHttpDoc(String content) {
        httpDocContent = content;
        httpDocGone = false;
    }

    @DELETE
    @Path("/http-doc")
    public void removeHttpDoc() {
        httpDocGone = true;
    }

    // --- feeding the endpoint-source pipeline via its Camel URI ------------------------------

    @POST
    @Path("/custom-feed/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> customFeed(@PathParam("documentId") String documentId, String content) {
        IngestResult result = producerTemplate.requestBodyAndHeader(
                "direct:custom-source", content, IngestHeaders.DOCUMENT_ID, documentId, IngestResult.class);
        return Map.of("outcome", result.outcome().label(), "segmentsWritten", result.segmentsWritten());
    }

    /** Sends a document WITHOUT the required id header — the DLC must receive the exchange. */
    @POST
    @Path("/custom-feed-without-id")
    @Consumes(MediaType.TEXT_PLAIN)
    public void customFeedWithoutId(String content) {
        producerTemplate.sendBody("direct:custom-source", content);
    }

    @GET
    @Path("/custom-dlq")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> customDlq() {
        return DlqRoute.DEAD_LETTERS;
    }

    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Long>> metrics() {
        return Map.of(
                "documents", metrics.documentsIngested(),
                "segments", metrics.segmentsWritten(),
                "failures", metrics.failures(),
                "replaced", metrics.replaced(),
                "skippedUnchanged", metrics.skippedUnchanged(),
                "deleted", metrics.deleted(),
                "deadLettered", metrics.deadLettered(),
                "staleRetained", metrics.staleRetained());
    }
}
