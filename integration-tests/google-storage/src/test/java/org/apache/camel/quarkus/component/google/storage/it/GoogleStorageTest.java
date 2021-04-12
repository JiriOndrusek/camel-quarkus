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
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.util.CollectionHelper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(GoogleStorageTestResource.class)
class GoogleStorageTest {

    private static final String DEST_BUCKET = "destBucket";
    private static final String TEST_BUCKET1 = "testBucket1";
    private static final String TEST_BUCKET2 = "testBucket2";
    private static final String FILE_NAME_007 = "file007";
    private static final String FILE_NAME_006 = "file006";

    @Test
    public void testConsumer() throws InterruptedException {
        //consumer - start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_BUCKET, TEST_BUCKET1)
                .queryParam(GoogleStorageResource.QUERY_POLLING_ACTION, "moveAfterRead")
                .queryParam(GoogleStorageResource.QUERY_DESTINATION_BUCKET, DEST_BUCKET)
                .post("/google-storage/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        //producer - putObject
        putObject("Sheldon", TEST_BUCKET1, FILE_NAME_007);

        //get result from direct (for pooling) with timeout
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_DIRECT, GoogleStorageResource.DIRECT_POLLING)
                .post("/google-storage/getFromDirect")
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

        //producer - getObject
        executeOperation(GoogleCloudStorageOperations.getObject,
                Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007),
                is("Sheldon"));
    }

    @Test
    public void testProducer() throws InterruptedException {

        //create object in testBucket
        putObject("Sheldon", TEST_BUCKET1, FILE_NAME_007);

        putObject("Irma", TEST_BUCKET2, FILE_NAME_006);

        //copy object to test_bucket2
        executeOperation(GoogleCloudStorageOperations.copyObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007,
                        GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, TEST_BUCKET2,
                        GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, FILE_NAME_007 + "_copy"),
                is("Sheldon"));

        //delete object in test_bucket
        executeOperation(GoogleCloudStorageOperations.deleteObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007),
                is(Boolean.toString(true)));

        //list buckets
        executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                both(containsString(TEST_BUCKET1)).and(containsString(TEST_BUCKET2)));

        //delete bucket
        executeOperation(GoogleCloudStorageOperations.deleteBucket, Collections.emptyMap(), is(Boolean.toString(true)));

        executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                not(containsString(TEST_BUCKET1)));

    }

    private void putObject(String sheldon, String testBucket1, String fileName007) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(sheldon)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, testBucket1)
                .queryParam(GoogleStorageResource.QUERY_OBJECT_NAME, fileName007)
                .post("/google-storage/putObject")
                .then()
                .statusCode(201)
                .body(is(fileName007));
    }

    private void executeOperation(GoogleCloudStorageOperations operation, Map<String, Object> parameters, Matcher matcher) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(parameters)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, TEST_BUCKET1)
                .queryParam(GoogleStorageResource.QUERY_OPERATION, operation)
                .post("/google-storage/operation")
                .then()
                .statusCode(200)
                .body(matcher);
    }

}
