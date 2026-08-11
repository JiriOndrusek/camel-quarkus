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

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The retrieval-side isolation hook: a {@code RagRetrievalFilterSupplier} bean filters every
 * retrieval through the produced augmentor — tenant metadata written at ingestion time becomes
 * an actual access control.
 */
@QuarkusTest
class RagRetrievalFilterTest {

    @AfterEach
    void clearFilter() {
        RestAssured.post("/rag-bridge/tenant-filter/none").then().statusCode(200);
    }

    @Test
    void tenantFilterIsolatesRetrieval() {
        seed("alpha", "The ALPHA-ONLY clause: tenants of type alpha may exceed the quota.");
        seed("beta", "The BETA-ONLY clause: tenants of type beta must not exceed the quota.");

        // unfiltered: both tenants' documents are retrievable
        List<String> unfiltered = augment("What does the clause say about the quota?");
        assertTrue(unfiltered.stream().anyMatch(text -> text.contains("ALPHA-ONLY")), "got: " + unfiltered);
        assertTrue(unfiltered.stream().anyMatch(text -> text.contains("BETA-ONLY")), "got: " + unfiltered);

        // filtered to alpha: beta's documents must be invisible
        RestAssured.post("/rag-bridge/tenant-filter/alpha").then().statusCode(200);
        List<String> filtered = augment("What does the clause say about the quota?");
        assertTrue(filtered.stream().anyMatch(text -> text.contains("ALPHA-ONLY")), "got: " + filtered);
        assertFalse(filtered.stream().anyMatch(text -> text.contains("BETA-ONLY")),
                "the tenant filter must isolate retrieval, got: " + filtered);
    }

    static void seed(String tenant, String text) {
        RestAssured.given().contentType(ContentType.TEXT).body(text)
                .post("/rag-bridge/seed/" + tenant)
                .then().statusCode(200);
    }

    static List<String> augment(String question) {
        return RestAssured.given().contentType(ContentType.TEXT).body(question)
                .post("/rag-bridge/augment")
                .then().statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
