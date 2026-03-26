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
class PqcNativeModeTest {

    @Test
    public void testBouncyCastleProviderAvailable() {
        // Verify BouncyCastlePQCProvider is registered in Security providers
        RestAssured.post("/pqc/provider/check")
                .then()
                .statusCode(200)
                .body(equalTo("available"));
    }

    @Test
    public void testAllAlgorithmsAvailable() {
        // Verify all required algorithms are accessible
        RestAssured.post("/pqc/algorithms/check")
                .then()
                .statusCode(200)
                .body(equalTo("all-available"));
    }

    @Test
    public void testNoMissingResources() {
        // Verify no "resource not found" issues in native mode
        // This is tested implicitly by running all the other tests successfully
        // If resources were missing, the provider or algorithms wouldn't be available

        // Test that basic operations work (indicates resources are properly registered)
        String signature = RestAssured.post("/pqc/sign/dilithium")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/dilithium")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }
}
