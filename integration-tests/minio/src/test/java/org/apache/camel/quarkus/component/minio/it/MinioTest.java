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

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.apache.camel.BindToRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(MinioTestResource.class)
class MinioTest {

    private String BUCKET_NAME = "mycamel";
    private long PART_SIZE = 50 * 1024 * 1024;

    @BindToRegistry("minioClient")
    MinioClient client = MinioClient.builder()
            .endpoint("http://" + System.getProperty(MinioResource.PARAM_SERVER_HOST), Integer.parseInt(System.getProperty(MinioResource.PARAM_SERVER_PORT)), false)
            .credentials(MinioResource.SERVER_ACCESS_KEY, MinioResource.SERVER_SECRET_KEY)
            .build();

    @Test
    public void test() {
        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/minio/post") //
                .then()
                .statusCode(201);

        Assertions.fail("Add some assertions to " + getClass().getName());

        RestAssured.get("/minio/get")
                .then()
                .statusCode(200);
    }

    @Test
    public void testConsumer() throws Exception {

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();

        //execute component server to wait for the result


//        String s1 = RestAssured.get("/minio/consumer")
//                .then()
//                .extract().asString();

        Future<String> fr1 = executor.submit(
                () -> RestAssured.get("/minio/consumer")
                        .then()
                        .extract().asString());

        sendViaClient();

        //wait for the result from the server
        await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            String result = fr1.get();
            Assertions.assertEquals("Dummy content", result);
        });
    }

    private void sendViaClient() {
        String dummyFile = "Dummy content";
        try (InputStream is = new ByteArrayInputStream((dummyFile.getBytes()))) {
            ObjectWriteResponse response = client.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object("test_name")
                            .contentType("text/xml")
                            .stream(is, -1, PART_SIZE)
                            .build());
//            GetObjectResponse test = minioClient.getObject(GetObjectArgs.builder().bucket("test").object(fileName).build());
//            return test.bucket() + "/" + test.object();
//            System.out.println(response);
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
