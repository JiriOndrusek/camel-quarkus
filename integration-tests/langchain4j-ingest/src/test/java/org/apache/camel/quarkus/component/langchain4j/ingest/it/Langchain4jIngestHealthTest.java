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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

/**
 * The readiness check: every pipeline's route started means UP; a stopped pipeline route flips
 * it DOWN naming the pipeline, and restarting recovers it.
 */
@QuarkusTest
class Langchain4jIngestHealthTest {

    @Test
    void readinessFollowsTheRoutes() {
        // readiness ramps up: Camel's consumer check stays DOWN until the file consumer's
        // first poll completed, so the initial UP is awaited rather than asserted
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.get("/q/health/ready")
                        .then().statusCode(200)
                        .body(containsString("camel-quarkus-langchain4j-ingest"))
                        .body(containsString("\"custom\"")));

        try {
            RestAssured.post("/langchain4j-ingest/route/custom/stop")
                    .then().statusCode(204);
            RestAssured.get("/q/health/ready")
                    .then().statusCode(503)
                    .body(containsString("camel-quarkus-langchain4j-ingest"))
                    .body(containsString("stopped"));
        } finally {
            RestAssured.post("/langchain4j-ingest/route/custom/start")
                    .then().statusCode(204);
        }

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.get("/q/health/ready")
                        .then().statusCode(200));
    }
}
