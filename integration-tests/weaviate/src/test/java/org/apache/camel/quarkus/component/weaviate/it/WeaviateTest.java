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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import org.apache.camel.Exchange;
import org.apache.camel.component.weaviate.WeaviateVectorDb;
import org.apache.camel.component.weaviate.WeaviateVectorDbAction;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfSystemProperties;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "weaviate.host", matches = ".+")
@EnabledIfEnvironmentVariable(named = "weaviate.apikey", matches = ".+")
class WeaviateTest {

    private static final String COLLECTION_NAME = "WeaviateCQCollection";




    @Test
    public void crud() {
        List<Float> values = Arrays.asList(1.0f, 2.0f, 3.0f);
        Map<String, String> properties = Map.of("sky", "blue", "age", "34");


        //tests
        createCollection();

        try {
            String entryId = createEntry(values, properties);

            queryById(entryId)
                    .body("result", Matchers.hasSize(1))
                    .body("result", Matchers.hasItem(entryId));

            updateById(entryId);



        } finally {
            deleteCollection();
        }
    }




//    @Test
//    @Order(7)
//    public void updateById() {
//
//        List<Float> elements = Arrays.asList(1.0f, 2.0f, 3.0f);
//
//        HashMap<String, String> map = new HashMap<String, String>();
//        map.put("dog", "dachshund");
//
//        Exchange result = fluentTemplate.to(
//                        "weaviate:test-collection?scheme={{weaviate.scheme}}&host={{weaviate.host}}&apiKey={{weaviate.apikey}}")
//                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.UPDATE_BY_ID)
//                .withBody(elements)
//                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION)
//                .withHeader(WeaviateVectorDb.Headers.INDEX_ID, CREATEID)
//                .withHeader(WeaviateVectorDb.Headers.PROPERTIES, map)
//                .request(Exchange.class);
//
//        assertThat(result).isNotNull();
//
//        Result<Boolean> res = (Result<Boolean>) result.getIn().getBody();
//        assertThat(!res.hasErrors());
//        assertThat(res.getResult() == true);
//        assertThat(result.getException()).isNull();
//    }





    //    @Test
    //    @Order(9)
    //    public void deleteById() {
    //
    //        Exchange result = fluentTemplate
    //                .to("weaviate:test-collection")
    //                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.DELETE_BY_ID)
    //                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION)
    //                .withHeader(WeaviateVectorDb.Headers.INDEX_ID, CREATEID)
    //                .request(Exchange.class);
    //
    //        assertThat(result).isNotNull();
    //        Result<Boolean> res = (Result<Boolean>) result.getIn().getBody();
    //
    //        assertThat(!res.hasErrors());
    //        assertThat(res.getResult() == true);
    //        assertThat(result.getException()).isNull();
    //    }
    //


    private void createCollection() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE_COLLECTION,
                        WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION_NAME))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }


    private void deleteCollection() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.DELETE_COLLECTION,
                        WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION_NAME))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }

    private String createEntry(List<Float> values, Map<String, String> properties) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE,
                WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION_NAME,
                WeaviateVectorDb.Headers.PROPERTIES, properties
        );

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


    public ValidatableResponse queryById(String id) {
        Map<String, Object> payload = Map.of(
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.QUERY_BY_ID,
                WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION_NAME,
                WeaviateVectorDb.Headers.INDEX_ID, id);


        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
    }

    private ValidatableResponse updateById(String id) {

        List<Float> elements = Arrays.asList(1.0f, 2.0f, 3.0f);

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("dog", "dachshund");

        Map<String, Object> payload = Map.of(
                WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.UPDATE_BY_ID,
                WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION_NAME,
                WeaviateVectorDb.Headers.INDEX_ID, id,
                WeaviateVectorDb.Headers.PROPERTIES, map);


        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
    }

}


