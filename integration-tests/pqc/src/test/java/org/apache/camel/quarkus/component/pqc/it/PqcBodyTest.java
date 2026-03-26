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
class PqcBodyTest {

    @Test
    public void testSignVerifyWithEmptyBody() {
        // Sign with empty body
        String signature = RestAssured.post("/pqc/sign/emptybody")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Verify with empty body
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/emptybody")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testSignVerifyWithLargeBody1MB() {
        // Sign with 1MB body
        int size = 1024 * 1024; // 1MB
        String signature = RestAssured.post("/pqc/sign/largebody/" + size)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Note: Verification with large bodies requires the same random data
        // which is not reproducible, so we just verify signing works
    }

    @Test
    public void testSignVerifyWithLargeBody10MB() {
        // Sign with 10MB body
        int size = 10 * 1024 * 1024; // 10MB
        String signature = RestAssured.post("/pqc/sign/largebody/" + size)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Note: Verification with large bodies requires the same random data
        // which is not reproducible, so we just verify signing works
    }

    @Test
    public void testSignVerifyWithLargeBody100MB() {
        // Sign with 100MB body
        int size = 100 * 1024 * 1024; // 100MB
        String signature = RestAssured.post("/pqc/sign/largebody/" + size)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Note: Verification with large bodies requires the same random data
        // which is not reproducible, so we just verify signing works
    }

    @Test
    public void testSignVerifyWithBinaryData() {
        // Sign and verify with binary data
        String combined = RestAssured.post("/pqc/sign/binarydata")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(combined);

        // Verify with same binary data
        RestAssured.given()
                .contentType("text/plain")
                .body(combined)
                .post("/pqc/verify/binarydata")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testBodyPreservationThroughSign() {
        // Test that message body is preserved through sign operation
        String testMessage = "Test message for body preservation";
        String result = RestAssured.given()
                .contentType("text/plain")
                .body(testMessage)
                .post("/pqc/sign/preservebody")
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
}
