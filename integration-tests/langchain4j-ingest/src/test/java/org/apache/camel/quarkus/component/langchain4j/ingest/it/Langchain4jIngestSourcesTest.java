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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The P5 sources: an {@code http} document synchronised by conditional-GET scan passes
 * (replace on change, delete on 404 — one URL is one document), and the {@code endpoint}
 * escape hatch (any Camel consumer URI feeds the pipeline).
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Langchain4jIngestSourcesTest {

    @Test
    @Order(1)
    void httpDocumentIsIngestedAndReplacedOnChange() {
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(searchWebdoc("Which torque wrench is mentioned?")
                        .stream().anyMatch(text -> text.contains("FALCON-9000")),
                        "the initial http document must be ingested"));

        RestAssured.given().contentType(ContentType.TEXT)
                .body("The web manual now mentions the FALCON-9500 torque wrench.")
                .post("/langchain4j-ingest/http-doc")
                .then().statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<String> hits = searchWebdoc("Which torque wrench is mentioned?");
                    assertTrue(hits.stream().anyMatch(text -> text.contains("FALCON-9500")),
                            "the changed document must be re-ingested (ETag changed), got: " + hits);
                    assertFalse(hits.stream().anyMatch(text -> text.contains("FALCON-9000")),
                            "the old version must be replaced, got: " + hits);
                });
    }

    @Test
    @Order(2)
    void httpDocumentGoneMeansDeleted() {
        RestAssured.delete("/langchain4j-ingest/http-doc").then().statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertFalse(searchWebdoc("Which torque wrench is mentioned?")
                            .stream().anyMatch(text -> text.contains("FALCON")),
                            "a 404 document must disappear from the knowledge base");
                    RestAssured.get("/langchain4j-ingest/metrics").then()
                            .body("deleted.webdoc", greaterThan(0));
                });
    }

    @Test
    @Order(3)
    void endpointSourceFeedsThePipeline() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The custom integration delivers the OMEGA-7 spec sheet.")
                .post("/langchain4j-ingest/custom-feed/integration/omega.txt")
                .then()
                .statusCode(200)
                .body("outcome", equalTo("ingested"))
                .body("segmentsWritten", greaterThan(0));

        RestAssured.given()
                .queryParam("q", "Which spec sheet is delivered?")
                .queryParam("store", "custom")
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .body("", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("OMEGA-7")));
    }

    static List<String> searchWebdoc(String query) {
        return RestAssured.given()
                .queryParam("q", query)
                .queryParam("store", "webdoc")
                .queryParam("max", 10)
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
