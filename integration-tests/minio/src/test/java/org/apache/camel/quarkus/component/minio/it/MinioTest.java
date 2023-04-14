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
package org.apache.camel.quarkus.component.minio.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(MinioTestResource.class)
class MinioTest {
    private static final long PART_SIZE = 50 * 1024 * 1024;

    private final String BUCKET_NAME = "mycamel";

    private MinioClient minioClient;

    @Test
    public void testConsumer() throws Exception {
        initClient();

        sendViaClient("Dummy content", "consumerObject");

        //wait for the result from the server
        await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            String result = RestAssured.get("/minio/consumer")
                    .then()
                    .extract().asString();
            Assertions.assertEquals("Dummy content", result);
        });
    }

    @Test
    public void testDeleteObject() throws Exception {
        initClient();

        sendViaClient("Dummy content", "testDeleteObject");

//        producerRequest(MinioOperations.listObjects, null, Collections.emptyMap())
//                .statusCode(200)
//                .body(containsString("item: testDeleteObject"));
//
//        producerRequest(MinioOperations.deleteObject, null,
//                Collections.singletonMap(MinioConstants.OBJECT_NAME, "testDeleteObject"))
//                        .statusCode(200)
//                        .body(containsString("true"));
//
//        producerRequest(MinioOperations.listObjects, null, Collections.emptyMap())
//                .statusCode(200)
//                .body(equalTo(""));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        initClient();
//
//        producerRequest(MinioOperations.listBuckets, null, Collections.emptyMap())
//                .statusCode(200)
//                .body(containsString("bucket: " + BUCKET_NAME));
//
//        producerRequest(MinioOperations.deleteBucket, null, Collections.emptyMap())
//                .statusCode(200);
//
//        producerRequest(MinioOperations.listBuckets, null, Collections.emptyMap())
//                .statusCode(200)
//                .body(equalTo(""));
    }

    @Test
    public void testBasicOperations() throws Exception {
//        producerRequest(null, "Hi Sheldon.", Collections.singletonMap(MinioConstants.OBJECT_NAME, "putName"))
//                .statusCode(200)
//                .body(containsString("Hi Sheldon"));
//
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(MinioConstants.OBJECT_NAME, "putName"))
                .body("Hi Sheldon.")
                .post("minio/operation2")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

//        producerRequest(MinioOperations.copyObject, null, CollectionHelper.mapOf(MinioConstants.OBJECT_NAME, "putName",
//                MinioConstants.DESTINATION_OBJECT_NAME, "copyName",
//                MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME))
//                        .statusCode(200);
//

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(
                        MinioConstants.MINIO_OPERATION, MinioOperations.copyObject,
                        MinioConstants.OBJECT_NAME, "putName",
                        MinioConstants.DESTINATION_OBJECT_NAME, "copyName",
                        MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME))
                .post("minio/operation2")
                .then()
                .statusCode(200);


//        producerRequest(MinioOperations.getObject, null, Collections.singletonMap(MinioConstants.OBJECT_NAME, "copyName"))
//                .statusCode(200)
//                .body(containsString("Hi Sheldon"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(MinioConstants.OBJECT_NAME, "copyName"))
                .post("minio/operation2")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));
    }

    @Test
    public void testGetViaPojo() throws Exception {
        initClient();
        initClient(BUCKET_NAME + "2");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(MinioConstants.OBJECT_NAME, "putViaPojoName"))
                .body("Hi Sheldon.")
                .post("minio/operation2")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(
                        MinioConstants.MINIO_OPERATION, MinioOperations.copyObject,
                        MinioConstants.OBJECT_NAME, "putViaPojoName",
                        MinioConstants.DESTINATION_OBJECT_NAME, "copyViaPojoName",
                        MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME + "2"))
                .body("Hi Sheldon.")
                .post("minio/operation2")
                .then()
                .statusCode(200);

        producerRequest(MinioOperations.getObject, BUCKET_NAME + "2",
                Collections.singletonMap(MinioConstants.OBJECT_NAME, "copyViaPojoName"),
                "/minio/getUsingPojo")
                        .statusCode(200)
                        .body(containsString("Hi Sheldon"));
    }

    private ValidatableResponse producerRequest(MinioOperations operation, String body, Map<String, String> params) {
        return producerRequest(operation, body, params, "/minio/operation");
    }

    private ValidatableResponse producerRequest(MinioOperations operation, String body, Map<String, String> params,
            String path) {
        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam(MinioConstants.MINIO_OPERATION, operation != null ? operation.name() : null);

        params.forEach((k, v) -> request.queryParam(k, v));

        return request.body(body == null ? "" : body)
                .post(path)
                .then();
    }

    private MinioClient initClient() throws Exception {
        return initClient(BUCKET_NAME);
    }

    private MinioClient initClient(String bucketName) throws Exception {
        if (minioClient == null) {
            minioClient = new MinioClientProducer().produceMinioClient();
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        return minioClient;
    }

    private void sendViaClient(String content, String objectName) {
        sendViaClient(minioClient, content, objectName);
    }

    private void sendViaClient(MinioClient client, String content, String objectName) {
        try (InputStream is = new ByteArrayInputStream((content.getBytes()))) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .contentType("text/xml")
                            .stream(is, -1, PART_SIZE)
                            .build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testGetObjectRange() throws Exception {
        MinioClient client = initClient();

        sendViaClient(client, "MinIO is a cloud storage server compatible with Amazon S3, ...", "element.txt");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getPartialObject,
                            MinioConstants.OFFSET, 1,
                            MinioConstants.LENGTH, 8,
                            MinioConstants.OBJECT_NAME, "element.txt"))
                .post("minio/operation2")
                .then()
                .statusCode(200)
                .body(containsString("inIO is"));
    }

    @Test
    public void testAUtocreateBucket() throws Exception {
        //creates bucket mycamel
        initClient();
        String nonExistingBucket1 = "nonexistingbucket1";
        String nonExistingBucket2 = "nonexistingbucket2";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets,
                        "autoCreateBucket", "true",
                        "bucket", nonExistingBucket1))
                .post("/minio/operation2")
                .then()
                .statusCode(200)
                .body(both(containsString("bucket: " + BUCKET_NAME)).and(containsString("bucket: " + nonExistingBucket1)));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("parameters", parameters(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets,
                        "autoCreateBucket", "false",
                        "bucket", nonExistingBucket2))
                .post("/minio/operation2")
                .then()
                .statusCode(500)
                .body(containsString("Failed to resolve endpoint"));
    }

    private static String parameters(Object... os) {
        return Stream.concat(Arrays.stream(new String[] {os[0].toString()}),
                        IntStream.range(1, os.length)
                            .mapToObj(i -> (i % 2 == 0 ? "," : ":") + os[i].toString())
                )
                .collect(Collectors.joining());
    }


}
