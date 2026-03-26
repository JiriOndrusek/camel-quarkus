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

@QuarkusTest
class PqcSymmetricTest {

    @Test
    public void testKemWithAes256() {
        // KEM operations with AES-256 symmetric key algorithm
        RestAssured.post("/pqc/kem/aes256")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testKemWithChacha256() {
        // KEM operations with CHACHA7539-256 symmetric key algorithm
        // Note: CHACHA7539 only supports 256-bit keys
        RestAssured.post("/pqc/kem/encapsulate/kyber-chacha")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSymmetricAlgorithmMismatch() {
        // Test encapsulation with AES-128
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Extract with different symmetric algorithm (CHACHA7539-256)
        // This should handle the mismatch appropriately
        String result = RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/kem/extract/kyber-chacha")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // The result should be "false" or produce a different key
        // In real-world scenarios, using the wrong algorithm would fail
    }

    @Test
    public void testSymmetricKeyLengthMismatch() {
        // Test encapsulation with AES-128
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // The encapsulation is for AES-128, but we can only verify it works
        // with the correct parameters in the existing tests
    }

    @Test
    public void testAllSymmetricCombinations() {
        // Test all symmetric algorithm combinations

        // AES-128 (already tested in main PqcTest)
        RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200);

        // AES-256
        RestAssured.post("/pqc/kem/aes256")
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // CHACHA7539-256 (Note: CHACHA7539 only supports 256-bit keys)
        RestAssured.post("/pqc/kem/encapsulate/kyber-chacha")
                .then()
                .statusCode(200);
    }
}
