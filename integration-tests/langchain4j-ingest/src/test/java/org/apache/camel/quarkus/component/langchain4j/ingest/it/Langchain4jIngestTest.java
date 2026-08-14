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

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class Langchain4jIngestTest {

    @Test
    void fileSourceIngestsDocumentsIntoTheStore() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("The warranty period for the Frobnicator X200 is twenty four months from purchase.")
                .post("/langchain4j-ingest/file/warranty.txt")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.given()
                        .queryParam("q", "How long is the Frobnicator warranty?")
                        .get("/langchain4j-ingest/search")
                        .then()
                        .statusCode(200)
                        .body("", hasItem(containsString("twenty four months"))));

        RestAssured.get("/langchain4j-ingest/metrics")
                .then()
                .statusCode(200)
                .body("documents.products", greaterThan(0))
                .body("segments.products", greaterThan(0));

        // the same counters exposed through Micrometer (optional-dependency listener)
        RestAssured.get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("cq_ingest_documents"));
    }

    @Test
    void ingressIngestsWithExplicitDocumentId() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Returns are accepted within thirty days with the original receipt.")
                .post("/langchain4j-ingest/ingress/policies/returns.txt")
                .then()
                .statusCode(200)
                .body("pipeline", equalTo("products"))
                .body("documentId", equalTo("policies/returns.txt"))
                .body("segmentsWritten", greaterThan(0));

        RestAssured.given()
                .queryParam("q", "Can I return a product?")
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .body("", hasItem(containsString("thirty days")));
    }

    @Test
    void ingressWithoutDocumentIdFails() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("some content")
                .post("/langchain4j-ingest/ingress-without-id")
                .then()
                .statusCode(400)
                .body(containsString("CamelAiIngestDocumentId"));
    }
}
