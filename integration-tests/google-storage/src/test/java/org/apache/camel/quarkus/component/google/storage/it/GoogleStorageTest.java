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
package org.apache.camel.quarkus.component.google.storage.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(GoogleStorageTestResource.class)
class GoogleStorageTest {

    private static final String DEST_BUCKET = "destBucket";
    private static final String TEST_BUCKET = "testBucket";
    private static final String FILE_NAME = "file007";

    @Test
    public void test() throws InterruptedException {
        //consumer - start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_BUCKET, TEST_BUCKET)
                .queryParam(GoogleStorageResource.QUERY_DESTINATION_BUCKET, DEST_BUCKET)
                .post("/google-storage/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        //producer - putObject
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .queryParam(GoogleStorageResource.QUERY_BUCKET, TEST_BUCKET)
                .queryParam(GoogleStorageResource.QUERY_OBJECT_NAME, FILE_NAME)
                .post("/google-storage/putObject")
                .then()
                .statusCode(201)
                .body(is(FILE_NAME));

        //get result from direct (for pooling) with timeout
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_DIRECT, GoogleStorageResource.DIRECT_POLLING)
                .post("/google-storage/getFromDirect")
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

        //producer - getObject
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(FILE_NAME)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, "destBucket")
                .post("/google-storage/getObject")
                .then()
                .statusCode(201)
                .body(is("Sheldon"));
    }
}
