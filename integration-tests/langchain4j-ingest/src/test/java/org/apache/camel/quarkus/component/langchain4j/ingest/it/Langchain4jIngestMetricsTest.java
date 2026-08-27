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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code cq.ingest.*} counters, published through Micrometer and read back over the
 * Prometheus endpoint: one ingestion counts documents and segments, a duplicate delivery counts
 * skipped, a blank one counts empty.
 */
@QuarkusTest
class Langchain4jIngestMetricsTest {

    @Test
    void countersAppearInPrometheus() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The XI-2 gauge measures depth.")
                .post("/langchain4j-ingest/feed/custom/metrics/xi.txt")
                .then().statusCode(200).body(Matchers.is("ingested"));

        RestAssured.given().contentType(ContentType.TEXT)
                .body("The XI-2 gauge allegedly measures pressure now.")
                .post("/langchain4j-ingest/feed/custom/metrics/xi.txt")
                .then().statusCode(200).body(Matchers.is("skipped"));

        RestAssured.given().contentType(ContentType.TEXT)
                .body("   ")
                .post("/langchain4j-ingest/feed/custom/metrics/blank.txt")
                .then().statusCode(200).body(Matchers.is("empty"));

        String metrics = RestAssured.get("/q/metrics")
                .then().statusCode(200).extract().asString();
        assertCounter(metrics, "cq_ingest_documents_total");
        assertCounter(metrics, "cq_ingest_segments_total");
        assertCounter(metrics, "cq_ingest_skipped_total");
        assertCounter(metrics, "cq_ingest_empty_total");
    }

    /** The counter must be present for the custom pipeline with a value of at least one. */
    private static void assertCounter(String metrics, String counter) {
        assertTrue(metrics.matches("(?s).*" + counter + "\\{pipeline=\"custom\",?\\} [1-9].*"),
                counter + " must be counted for the custom pipeline, got:\n" + metrics);
    }
}
