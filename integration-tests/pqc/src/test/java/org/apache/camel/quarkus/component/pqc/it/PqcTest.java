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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PqcTest {

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testSignAndVerifyWithDilithium() {
        // Sign operation using Camel PQC component
        String signature = RestAssured.post("/pqc/sign/dilithium")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testSignAndVerifyWithFalcon() {
        // Sign operation using Camel PQC component
        String signature = RestAssured.post("/pqc/sign/falcon")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/falcon")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testSignAndVerifyWithSphincs() {
        // Sign operation using Camel PQC component
        String signature = RestAssured.post("/pqc/sign/sphincs")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/sphincs")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testKemEncapsulationWithKyberAes() {
        // Generate encapsulation using Camel PQC component
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(encapsulation);
        assertFalse(encapsulation.isEmpty());

        // Extract secret key from encapsulation using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/kem/extract/kyber-aes")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testKemExtractToHeaderWithKyberAes() {
        // Generate encapsulation using Camel PQC component
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-aes")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(encapsulation);
        assertFalse(encapsulation.isEmpty());

        // Extract secret key to header using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/kem/extract-to-header/kyber-aes")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    // Disabled: BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution.
    @Disabled("BouncyCastle 1.83 algorithm naming and header compatibility issues with Camel PQC component need resolution")
    @Test
    public void testKemEncapsulationWithKyberChacha() {
        // Generate encapsulation using Camel PQC component with CHACHA7539
        String encapsulation = RestAssured.post("/pqc/kem/encapsulate/kyber-chacha")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(encapsulation);
        assertFalse(encapsulation.isEmpty());

        // Extract secret key from encapsulation
        RestAssured.given()
                .contentType("text/plain")
                .body(encapsulation)
                .post("/pqc/kem/extract/kyber-chacha")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }
}
