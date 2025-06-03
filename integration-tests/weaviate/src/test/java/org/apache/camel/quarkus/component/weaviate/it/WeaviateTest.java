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
package org.apache.camel.quarkus.component.weaviate.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.component.weaviate.WeaviateVectorDb;
import org.apache.camel.component.weaviate.WeaviateVectorDbAction;
import org.hamcrest.Matchers;
import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WeaviateTestResource.class)
class WeaviateTest {

    @Test
    public void simpleCrud() {
        String collectionName = "WeaviateCQCollectionCrud";
        List<Float> values = Arrays.asList(1.0f, 2.0f, 3.0f);
        Map<String, String> properties = Map.of("sky", "blue", "age", "34");
        Map<String, String> updatedProperties = Map.of("dog", "dachshund");

        createCollection(collectionName);

        try {
            String id = createEntry(collectionName, values, properties);

            queryById(collectionName, id)
                    .body("result", Matchers.aMapWithSize(1))
                    .body("result." + id, Matchers.aMapWithSize(2));

            updateById(collectionName, id, values, updatedProperties);

            queryById(collectionName, id)
                    .body("result", Matchers.aMapWithSize(1))
                    .body("result." + id, Matchers.aMapWithSize(3));

            deleteById(collectionName, id);

            queryById(collectionName, id)
                    .body("result", Matchers.nullValue());

        } finally {
            deleteCollection(collectionName);
        }
    }

    @Test
    public void queryByVector() {

    }

    private void createCollection(String name) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE_COLLECTION,
                        WeaviateVectorDb.Headers.COLLECTION_NAME, name))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }

    private void deleteCollection(String name) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.DELETE_COLLECTION,
                        WeaviateVectorDb.Headers.COLLECTION_NAME, name))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }

    private String createEntry(String collectionName, List<Float> values, Map<String, String> properties) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE,
                WeaviateVectorDb.Headers.COLLECTION_NAME, collectionName,
                WeaviateVectorDb.Headers.PROPERTIES, properties);

        String createdId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .extract().path("result");

        Assertions.assertNotNull(createdId);

        return createdId;
    }

    public ValidatableResponse queryById(String collectionName, String id) {
        Map<String, Object> payload = Map.of(
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.QUERY_BY_ID,
                WeaviateVectorDb.Headers.COLLECTION_NAME, collectionName,
                WeaviateVectorDb.Headers.INDEX_ID, id);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
    }

    private ValidatableResponse updateById(String collectionName, String id, List<Float> values,
            Map<String, String> properties) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.UPDATE_BY_ID,
                WeaviateVectorDb.Headers.COLLECTION_NAME, collectionName,
                WeaviateVectorDb.Headers.INDEX_ID, id,
                WeaviateVectorDb.Headers.PROPERTIES, properties);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
    }

    public void deleteById(String collectionName, String id) {

        Map<String, Object> payload = Map.of(
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.DELETE_BY_ID,
                WeaviateVectorDb.Headers.COLLECTION_NAME, collectionName,
                WeaviateVectorDb.Headers.INDEX_ID, id);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then().statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }

}
