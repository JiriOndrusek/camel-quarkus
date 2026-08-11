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
