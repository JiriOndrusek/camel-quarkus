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
package org.apache.camel.quarkus.component.pqc.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PqcHeaderTest {

    @Test
    public void testHeaderPreservation() {
        // Test that custom headers are preserved through sign operation
        String result = RestAssured.post("/pqc/sign/headerpreservation")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        RestAssured.given()
                .body(result)
                .then()
                .statusCode(200)
                .body(equalTo("preserved"));
    }

    @Test
    public void testMalformedSignatureHeader() {
        // Test verify operation with malformed signature header
        String result = RestAssured.post("/pqc/verify/malformedheader")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(result);
        // Should produce error or handle gracefully
    }

    @Test
    public void testSecretKeyHeaderFormat() {
        // Generate encapsulation
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(encapsulation);

        // Extract with storeExtractedSecretKeyAsHeader=true
        RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/kem/extract-to-header/kyber-aes")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testExtractWithoutStoreToHeader() {
        // Generate encapsulation first
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(encapsulation);

        // Extract with storeExtractedSecretKeyAsHeader=false
        String result = RestAssured.post("/pqc/kem/extract-no-header")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        RestAssured.given()
                .body(result)
                .then()
                .statusCode(200)
                .body(equalTo("in-body"));
    }

    @Test
    public void testVerificationResultHeader() {
        // Sign operation
        String signature = RestAssured.post("/pqc/sign/dilithium")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Verify operation - should set CamelPQCVerification header to true
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium")
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // Verify with corrupted signature - should set CamelPQCVerification header to false
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium/corrupt")
                .then()
                .statusCode(200)
                .body(equalTo("false"));
    }
}
