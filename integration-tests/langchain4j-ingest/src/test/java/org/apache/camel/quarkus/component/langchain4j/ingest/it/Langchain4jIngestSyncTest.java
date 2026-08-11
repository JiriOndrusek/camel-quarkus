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

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sync-mode promise, end to end against a running application with a PostgreSQL-backed
 * ledger (Dev Services): an edited file replaces its previous vectors instead of joining them,
 * and an untouched-in-content file costs nothing.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Langchain4jIngestSyncTest {

    @Test
    @Order(1)
    void editedFileReplacesItsPreviousVectors() {
        writeManual("guide.txt",
                "The Frobnicator X200 ships with a EUROPA-MK1 power supply.\n"
                        + "Never immerse the device in water.\n"
                        + "The onboard reactor is fictional and this line will be removed.");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(searchManuals("What power supply does it ship with?")
                        .stream().anyMatch(text -> text.contains("EUROPA-MK1")), "v1 must be ingested"));

        writeManual("guide.txt",
                "The Frobnicator X200 ships with a EUROPA-MK2 power supply.\n"
                        + "Never immerse the device in water.");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<String> hits = searchManuals("What power supply does it ship with?");
                    assertTrue(hits.stream().anyMatch(text -> text.contains("EUROPA-MK2")),
                            "v2 must be present, got: " + hits);
                    assertFalse(hits.stream().anyMatch(text -> text.contains("EUROPA-MK1")),
                            "v1 must be REPLACED, not joined, got: " + hits);
                    assertFalse(hits.stream().anyMatch(text -> text.contains("reactor")),
                            "the removed line must not survive the shrink, got: " + hits);
                });

        RestAssured.get("/langchain4j-ingest/metrics")
                .then()
                .statusCode(200)
                .body("replaced.manuals", greaterThan(0));
    }

    @Test
    @Order(2)
    void rewritingSameContentIsSkippedByChangeDetection() {
        // same content, new mtime: tier-1 fingerprint misses, tier-2 content hash catches it
        writeManual("guide.txt",
                "The Frobnicator X200 ships with a EUROPA-MK2 power supply.\n"
                        + "Never immerse the device in water.");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.get("/langchain4j-ingest/metrics")
                        .then()
                        .statusCode(200)
                        .body("skippedUnchanged.manuals", greaterThan(0)));
    }

    @Test
    @Order(3)
    void deletedFileDisappearsFromTheKnowledgeBase() {
        writeManual("obsolete.txt", "The ACME-Z9 accessory kit is compatible with all models.");
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(searchManuals("Which accessory kit is compatible?")
                        .stream().anyMatch(text -> text.contains("ACME-Z9"))));

        RestAssured.given().queryParam("pipeline", "manuals")
                .delete("/langchain4j-ingest/file/obsolete.txt")
                .then().statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertFalse(searchManuals("Which accessory kit is compatible?")
                            .stream().anyMatch(text -> text.contains("ACME-Z9")),
                            "a document deleted at the source must disappear from the knowledge base");
                    RestAssured.get("/langchain4j-ingest/metrics").then()
                            .body("deleted.manuals", greaterThan(0));
                });
    }

    @Test
    @Order(4)
    void apiDeleteSticksWhileTheFileStillExists() {
        writeManual("held.txt", "The LEGAL-HOLD-X7 clause applies to all contracts.");
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(searchManuals("Which clause applies?")
                        .stream().anyMatch(text -> text.contains("LEGAL-HOLD-X7"))));

        RestAssured.given().contentType(ContentType.TEXT)
                .post("/langchain4j-ingest/ops/delete/manuals/held.txt")
                .then().statusCode(200);

        // the file is STILL in the watched directory; several passes must not resurrect it
        Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(searchManuals("Which clause applies?")
                        .stream().anyMatch(text -> text.contains("LEGAL-HOLD-X7")),
                        "an explicit delete must survive source passes — a legal hold that reverts "
                                + "is worse than none"));

        RestAssured.given().contentType(ContentType.TEXT)
                .post("/langchain4j-ingest/ops/unsuppress/manuals/held.txt")
                .then().statusCode(200);
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(searchManuals("Which clause applies?")
                        .stream().anyMatch(text -> text.contains("LEGAL-HOLD-X7")),
                        "unsuppress must hand the document back to the source"));
    }

    @Test
    @Order(5)
    void apiCorrectionPinsAgainstSourceReversion() {
        writeManual("spec.txt", "The maximum load is TEN kilograms.");
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(searchManuals("What is the maximum load?")
                        .stream().anyMatch(text -> text.contains("TEN"))));

        RestAssured.given().contentType(ContentType.TEXT)
                .body("The maximum load is FIFTEEN kilograms.")
                .post("/langchain4j-ingest/ops/upsert/manuals/spec.txt")
                .then().statusCode(200);

        // the outdated file is still in the directory; passes must not revert the correction
        Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<String> hits = searchManuals("What is the maximum load?");
                    assertTrue(hits.stream().anyMatch(text -> text.contains("FIFTEEN")),
                            "the correction must be live, got: " + hits);
                    assertFalse(hits.stream().anyMatch(text -> text.contains("TEN kilograms")),
                            "the source version must not silently revert the correction, got: " + hits);
                });
    }

    static void writeManual(String name, String content) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("pipeline", "manuals")
                .body(content)
                .post("/langchain4j-ingest/file/" + name)
                .then()
                .statusCode(204);
    }

    static List<String> searchManuals(String query) {
        return RestAssured.given()
                .queryParam("q", query)
                .queryParam("store", "manuals")
                .queryParam("max", 10)
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
