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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(GoogleStorageTestResource.class)
class GoogleStorageTest {

//    @Test
    public void testGetObject() {
        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/google-storage/post") //
                .then()
                .statusCode(201)
                .body(is("hello"));;
//
//        Assertions.fail("Add some assertions to " + getClass().getName());

//        RestAssured.get("/google-storage/get")
//                .then()
//                .statusCode(200);
    }

    @Test
    public void testPutGetObject() {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .queryParam(GoogleStorageResource.QUERY_PARAM_OBJECT_NAME, "object01")
                .post("/google-storage/putObject") //
                .then()
                .statusCode(201)
                .body(is("object01"));

        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("object01")
                .post("/google-storage/getObject") //
                .then()
                .statusCode(201)
                .body(is("Sheldon"));;
    }

//    @Test
//    public void test2() {
//        String port = System.getProperty(GoogleStorageTestResource.AAA);
//        Storage storage = StorageOptions.newBuilder()
//                .setHost("http://localhost:" + port)
//                .build()
//                .getService();
//
//        Bucket bucket = storage.get("my_bucket");
//
//        Blob result = bucket.get("my_file.txt");
//
//        System.out.println(new String(result.getContent()));
//    }

}
