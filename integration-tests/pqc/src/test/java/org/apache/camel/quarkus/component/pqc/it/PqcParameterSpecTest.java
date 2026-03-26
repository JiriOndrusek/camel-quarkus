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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PqcParameterSpecTest {

    @Test
    public void testSignAndVerifyWithDilithium3() {
        // Sign operation with dilithium3 parameter spec
        String signature = RestAssured.post("/pqc/sign/dilithium3")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation with dilithium3 parameter spec
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium3")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSignAndVerifyWithDilithium5() {
        // Sign operation with dilithium5 parameter spec
        String signature = RestAssured.post("/pqc/sign/dilithium5")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation with dilithium5 parameter spec
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium5")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSignAndVerifyWithFalcon1024() {
        // Sign operation with falcon_1024 parameter spec
        String signature = RestAssured.post("/pqc/sign/falcon1024")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation with falcon_1024 parameter spec
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/falcon1024")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testKemWithKyber768() {
        // KEM operations with kyber768 parameter spec
        RestAssured.post("/pqc/kem/kyber768")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testKemWithKyber1024() {
        // KEM operations with kyber1024 parameter spec
        RestAssured.post("/pqc/kem/kyber1024")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSphincsVariantsSha2_128s() {
        // Sign and verify with SPHINCS+ sha2_128s variant
        String signature = RestAssured.post("/pqc/sign/sphincs/sha2_128s")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs/sha2_128s")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSphincsVariantsSha2_192f() {
        // Sign and verify with SPHINCS+ sha2_192f variant
        String signature = RestAssured.post("/pqc/sign/sphincs/sha2_192f")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs/sha2_192f")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSphincsVariantsSha2_256f() {
        // Sign and verify with SPHINCS+ sha2_256f variant
        String signature = RestAssured.post("/pqc/sign/sphincs/sha2_256f")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs/sha2_256f")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSphincsVariantsShake_128f() {
        // Sign and verify with SPHINCS+ shake_128f variant
        String signature = RestAssured.post("/pqc/sign/sphincs/shake_128f")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs/shake_128f")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSphincsVariantsShake_256s() {
        // Sign and verify with SPHINCS+ shake_256s variant
        String signature = RestAssured.post("/pqc/sign/sphincs/shake_256s")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs/shake_256s")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }
}
