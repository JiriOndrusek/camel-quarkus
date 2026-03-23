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
class PqcTest {

    @Test
    public void loadComponentPqc() {
        RestAssured.get("/pqc/load/component/pqc")
                .then()
                .statusCode(200);
    }

    @Test
    public void mlDsaSignAndVerify() {
        // Sign a message using ML-DSA (Dilithium)
        String signature = RestAssured.post("/pqc/mldsa/sign")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Verify the signature is not null and not empty
        org.junit.jupiter.api.Assertions.assertNotNull(signature);
        org.junit.jupiter.api.Assertions.assertFalse(signature.isEmpty());

        // Verify the signature
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/mldsa/verify")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void mlKemKeyEncapsulation() {
        // Encapsulate a key using ML-KEM (Kyber)
        String encapsulation = RestAssured.post("/pqc/mlkem/encapsulate")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Verify the encapsulation is not null and not empty
        org.junit.jupiter.api.Assertions.assertNotNull(encapsulation);
        org.junit.jupiter.api.Assertions.assertFalse(encapsulation.isEmpty());

        // Extract the key from the encapsulation
        RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/mlkem/extract")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }
}
