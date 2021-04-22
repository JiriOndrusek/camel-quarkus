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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.util.CollectionHelper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(GoogleStorageTestResource.class)
class GoogleStorageTest {

    private static final String DEST_BUCKET = "camel_quarkus_test_dest_bucket";
    private static final String TEST_BUCKET1 = "camel_quarkus_test_bucket1";
    private static final String TEST_BUCKET2 = "camel_quarkus_test_bucket2";
    private static final String TEST_BUCKET3 = "camel_quarkus_test_bucket3";
    private static final String FILE_NAME_007 = "file007";
    private static final String FILE_NAME_006 = "file006";

    @BeforeEach
    public void beforeEach() {
        RestAssured.given().get("/google-storage/loadComponent").then().statusCode(200);
    }

    @AfterEach
    public void afterEach() {
        //clean after test
        deleteBuckets(TEST_BUCKET1, TEST_BUCKET2, TEST_BUCKET3, DEST_BUCKET);
    }

    @Test
    public void testConsumer() throws InterruptedException {
        //consumer - start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_BUCKET, TEST_BUCKET3)
                .queryParam(GoogleStorageResource.QUERY_POLLING_ACTION, "moveAfterRead")
                .queryParam(GoogleStorageResource.QUERY_DESTINATION_BUCKET, DEST_BUCKET)
                .post("/google-storage/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(5000);

        //producer - putObject
        putObject("Sheldon", TEST_BUCKET3, FILE_NAME_007);

        //get result from direct (for pooling) with timeout
        RestAssured.given()
                .queryParam(GoogleStorageResource.QUERY_DIRECT, GoogleStorageResource.DIRECT_POLLING)
                .post("/google-storage/getFromDirect")
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

        //producer - getObject
        executeOperation(DEST_BUCKET, GoogleCloudStorageOperations.getObject,
                Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007),
                is("Sheldon"));
    }

    @Test
    public void testProducer() {
        //delete existing buckets t - only on real account - Deleting buckets is not (yet) supported by fsouza/fake-gcs-server.
        if (GoogleStorageHelper.isRealAccount()) {
            String buckets = executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                    null);
            List<String> bucketsToDelete = Arrays.stream(buckets.split(","))
                    .filter(b -> b.equals(TEST_BUCKET1) || b.equals(TEST_BUCKET2))
                    .collect(Collectors.toList());
            if (!bucketsToDelete.isEmpty()) {
                bucketsToDelete.forEach(
                        b -> executeOperation(b, GoogleCloudStorageOperations.deleteBucket, Collections.emptyMap(),
                                is(Boolean.toString(true))));
            }
        }

        //create object in testBucket
        putObject("Sheldon", TEST_BUCKET1, FILE_NAME_007);

        putObject("Irma", TEST_BUCKET2, FILE_NAME_006);

        //copy object to test_bucket2
        executeOperation(GoogleCloudStorageOperations.copyObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007,
                        GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, TEST_BUCKET2,
                        GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, FILE_NAME_007 + "_copy"),
                is("Sheldon"));

        //GetObject
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.getObject,
                Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007 + "_copy"),
                is("Sheldon"));

        //list buckets
        executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                both(containsString(TEST_BUCKET1)).and(containsString(TEST_BUCKET2)));

        //deleteObject
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.deleteObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_006),
                is(Boolean.toString(true)));

        //ListObjects
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.listObjects, Collections.emptyMap(),
                containsString(FILE_NAME_007 + "_copy"));

        //CreateDownloadLink - requires authentication
        if (GoogleStorageHelper.isRealAccount()) {
            executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.createDownloadLink,
                    Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007 + "_copy"),
                    startsWith("http"));
        }
    }

    private void deleteBuckets(String... buckets) {
        //delete existing buckets - only on real account - Deleting buckets is not (yet) supported by fsouza/fake-gcs-server.
        Set<String> bucketSet = new HashSet<>(Arrays.asList(buckets));
        if (GoogleStorageHelper.isRealAccount()) {
            String realBuckets = executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                    null);
            List<String> bucketsToDelete = Arrays.stream(realBuckets.split(","))
                    .filter(b -> bucketSet.contains(b))
                    .collect(Collectors.toList());
            if (!bucketsToDelete.isEmpty()) {
                bucketsToDelete.forEach(
                        b -> executeOperation(b, GoogleCloudStorageOperations.deleteBucket, Collections.emptyMap(),
                                is(Boolean.toString(true))));
            }
        }
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

    private static String executeOperation(GoogleCloudStorageOperations operation, Map<String, Object> parameters,
            Matcher matcher) {
        return executeOperation(TEST_BUCKET1, operation, parameters, matcher);
    }

    private static String executeOperation(String bucketName, GoogleCloudStorageOperations operation,
            Map<String, Object> parameters, Matcher matcher) {
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(parameters)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, bucketName)
                .queryParam(GoogleStorageResource.QUERY_OPERATION, operation)
                .post("/google-storage/operation")
                .then()
                .statusCode(200);

        if (matcher != null) {
            response.body(matcher);
        }

        return response.extract().asString();
    }

}
