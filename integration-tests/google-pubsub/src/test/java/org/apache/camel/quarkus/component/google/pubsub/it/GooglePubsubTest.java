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
package org.apache.camel.quarkus.component.google.pubsub.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(GooglePubsubTestResource.class)
class GooglePubsubTest {

    //@Test
    public void pubsubTopicProduceConsume() {
        String message = "Hello Camel Quarkus Google PubSub";

        RestAssured.given()
                .body(message)
                .post("/google-pubsub")
                .then()
                .statusCode(201);

        RestAssured.get("/google-pubsub")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void jacksonSerializer() {
        String fruitName = "Apple";

        RestAssured.given()
                .body(fruitName)
                .post("/google-pubsub/pojo")
                .then()
                .statusCode(201);

        RestAssured.get("/google-pubsub/pojo")
                .then()
                .statusCode(200)
                .body("name", is(fruitName));
    }
}
