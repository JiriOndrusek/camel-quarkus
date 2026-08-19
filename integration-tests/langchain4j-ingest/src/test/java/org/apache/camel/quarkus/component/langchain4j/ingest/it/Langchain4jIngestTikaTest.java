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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code tika} parser: a PDF dropped into the watched directory is parsed to text in-process
 * and ingested like any text file, the file name as the document id. The fixture is a checked-in
 * one-page PDF carrying the asserted sentence, the same pattern the tika and docling test
 * modules use.
 */
@QuarkusTest
class Langchain4jIngestTikaTest {

    @Test
    void pdfIsParsedAndIngested() throws Exception {
        RestAssured.given().contentType(ContentType.BINARY)
                .body(getClass().getResourceAsStream("/pump.pdf").readAllBytes())
                .post("/langchain4j-ingest/binary/reports/pump.pdf")
                .then().statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Map<String, String> hit = Langchain4jIngestTest.hit(
                            "What does the pump tolerate?", "reports", "DELTA-5");
                    assertNotNull(hit, "the PDF must be parsed and its text ingested");
                    assertEquals("reports", hit.get("pipeline"));
                    assertEquals("pump.pdf", hit.get("documentId"));
                });
    }

    /**
     * A structured format (HTML, from the parser module camel-tika itself ships): adjacent block
     * elements must not glue into one word in the extracted text, and the {@code <head><title>}
     * must not be ingested — the parse lifts the body subtree only. The fixture keeps its two
     * paragraphs adjacent with no whitespace between them, so any separation comes from the
     * parse, not from the fixture's own formatting.
     */
    @Test
    void htmlBlocksStaySeparatedAndTitleIsDropped() throws Exception {
        RestAssured.given().contentType(ContentType.BINARY)
                .body(getClass().getResourceAsStream("/notice.html").readAllBytes())
                .post("/langchain4j-ingest/binary/reports/notice.html")
                .then().statusCode(204);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertNotNull(
                        Langchain4jIngestTest.hit("What opens the second paragraph?", "reports", "OMEGA-3"),
                        "the HTML must be parsed and its text ingested"));

        List<Map<String, String>> hits = Langchain4jIngestTest.hits("maintenance notice", "reports");
        // both paragraphs fit one segment, so the glue check is exercised within a single text
        assertTrue(hits.stream().anyMatch(
                hit -> hit.get("text").contains("GAMMA-7") && hit.get("text").contains("OMEGA-3")),
                "both paragraphs must land in one segment");
        assertTrue(hits.stream().noneMatch(hit -> hit.get("text").contains("GAMMA-7OMEGA-3")),
                "adjacent block elements must stay separated in the extracted text");
        assertTrue(hits.stream().noneMatch(hit -> hit.get("text").contains("Should not be ingested")),
                "the title must not be ingested");
    }
}
