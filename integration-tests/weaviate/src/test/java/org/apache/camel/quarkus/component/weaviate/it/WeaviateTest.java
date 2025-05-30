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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@QuarkusTestResource(WeaviateTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeaviateTest {

    @Test
    @Order(1)
    public void createCollection() {
        RestAssured.get("/weaviate/createCollection/test-collection")
                .then()
                .statusCode(200)
                .body(Matchers.is("false"));

        //        assertThat(result).isNotNull();
        //        Result<Boolean> res = (Result<Boolean>) result.getIn().getBody();
        //        assertThat(!res.hasErrors());
        //        assertThat(res.getResult() == true);
        //        assertThat(result.getException()).isNull();
    }
    //
    //    @Test
    //    @Order(2)
    //    public void create() {
    //
    //        List<Float> elements = Arrays.asList(1.0f, 2.0f, 3.0f);
    //
    //        HashMap<String, String> map = new HashMap<String, String>();
    //        map.put("sky", "blue");
    //        map.put("age", "34");
    //
    //        Exchange result = fluentTemplate
    //                .to("weaviate:test-collection")
    //                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE)
    //                .withBody(elements)
    //                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION)
    //                .withHeader(WeaviateVectorDb.Headers.PROPERTIES, map)
    //                .request(Exchange.class);
    //
    //        assertThat(result).isNotNull();
    //
    //        Result<WeaviateObject> res = (Result<WeaviateObject>) result.getIn().getBody();
    //        CREATEID = res.getResult().getId();
    //
    //        assertThat(!res.hasErrors());
    //        assertThat(res != null);
    //    }
    //
    //    @Test
    //    @Order(8)
    //    public void queryByVector() {
    //
    //        List<Float> elements = Arrays.asList(1.0f, 2.0f, 3.2f);
    //
    //        HashMap<String, String> map = new HashMap<String, String>();
    //        map.put("sky", "blue");
    //
    //        Exchange result = fluentTemplate
    //                .to("weaviate:test-collection")
    //                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.QUERY)
    //                .withBody(
    //                        elements)
    //                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION)
    //                .withHeader(WeaviateVectorDb.Headers.QUERY_TOP_K, 20)
    //                .withHeader(WeaviateVectorDb.Headers.FIELDS, map)
    //                .request(Exchange.class);
    //
    //        assertThat(result).isNotNull();
    //        List<Float> vector = (List<Float>) result.getIn().getBody();
    //        assertThat(vector.get(0) == 1.0f);
    //        assertThat(vector.get(1) == 2.0f);
    //        assertThat(vector.get(2) == 3.0f);
    //    }
    //
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
    //    @Test
    //    @Order(10)
    //    public void deleteCollection() {
    //        Exchange result = fluentTemplate
    //                .to("weaviate:test-collection")
    //                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.DELETE_COLLECTION)
    //                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, COLLECTION)
    //                .request(Exchange.class);
    //
    //        assertThat(result).isNotNull();
    //        Result<Boolean> res = (Result<Boolean>) result.getIn().getBody();
    //        assertThat(!res.hasErrors());
    //        assertThat(res.getResult() == true);
    //        assertThat(result.getException()).isNull();
    //    }

}
