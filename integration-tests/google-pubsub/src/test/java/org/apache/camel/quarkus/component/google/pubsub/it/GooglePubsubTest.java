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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(GoogleCloudTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) //required because https://github.com/apache/camel-quarkus/issues/3957
class GooglePubsubTest {
    private static final Logger LOG = Logger.getLogger(GooglePubsubTest.class);

    @Test
    @Order(1)
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
                .body(Matchers.is(message));
    }

    @Test
    @Order(2)
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
                .body("name", Matchers.is(fruitName));

    }

    @Test
    @Order(3)
    public void testGrouped() throws Exception {
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.GROUP_DIRECT_AGGREGATOR)
                .body("body1")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);

        RestAssured.given()
                .get("/google-pubsub/receive/subscription/google-pubsub.grouped-subscription-name")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.GROUP_DIRECT_AGGREGATOR)
                .body("body2")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);

        Set<String> results = new HashSet<>();
        results.add(RestAssured.given()
                .get("/google-pubsub/receive/subscription/google-pubsub.grouped-subscription-name")
                .then()
                .statusCode(200).extract().asString());

        results.add(RestAssured.given()
                .get("/google-pubsub/receive/subscription/google-pubsub.grouped-subscription-name")
                .then()
                .statusCode(200).extract().asString());

        Assertions.assertTrue(results.contains("body1"));
        Assertions.assertTrue(results.contains("body2"));

    }

    //Disabled on real account because of https://issues.apache.org/jira/browse/CAMEL-18277
    @DisabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+")
    // https://github.com/apache/camel-quarkus/issues/3944
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @Test
    @Order(4)
    public void testOrdering() throws Exception {
        LOG.info("Start of the ordering test");
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("1")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("2")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("3")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("4")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("5")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ORDERING_DIRECT_IN)
                .body("6")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        LOG.info("All messages were sent");

        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("1"));
        LOG.info("Message \"1\" received.");
        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("2"));
        LOG.info("Message \"2\" received.");
        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("3"));
        LOG.info("Message \"3\" received.");
        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("4"));
        LOG.info("Message \"4\" received.");
        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("5"));
        LOG.info("Message \"5\" received.");
        await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured.given()
                .get("/google-pubsub/receive/subscriptionOrdering/google-pubsub.ordering-subscription-name")
                .then()
                .statusCode(200)
                .extract().asString(),
                Matchers.is("6"));
        LOG.info("Message \"6\" received.");

    }

    @Test
    @Order(5)
    public void testAck() throws Exception {
        LOG.info("Start of the acking test");
        //enable ack
        RestAssured.given()
                .body(false)
                .post("/google-pubsub/setFail")
                .then()
                .statusCode(201);
        //successful run
        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ACK_DIRECT_IN)
                .body("1")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        LOG.info("Message \"1\" was sent and should be Acked.");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> RestAssured.given()
                .get("/google-pubsub/receive/mock/" + GooglePubSubRoutes.ACK_MOCK_RESULT)
                .then()
                .statusCode(200)
                .body(Matchers.is("1")));
        LOG.info("Message \"1\" was received.");
        //once message is received, reset mock
        RestAssured.given()
                .get("/google-pubsub/resetMock/" + GooglePubSubRoutes.ACK_MOCK_RESULT)
                .then()
                .statusCode(201);
        //failing run
        //disable ack
        RestAssured.given()
                .body(true)
                .post("/google-pubsub/setFail")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("toEndpoint", GooglePubSubRoutes.ACK_DIRECT_IN)
                .body("2")
                .post("/google-pubsub/sendToEndpoint")
                .then()
                .statusCode(201);
        LOG.info("Message \"2\" was sent and should be Nacked.");
        //wait to be sure that the nacked message was not delivered
        Thread.sleep(1000);

        RestAssured.given()
                .get("/google-pubsub/receive/mock/" + GooglePubSubRoutes.ACK_MOCK_RESULT)
                .then()
                .statusCode(200)
                .body(Matchers.is(""));
        LOG.info("Message \"2\" was not delivered.");

        //enable ack
        RestAssured.given()
                .body(false)
                .post("/google-pubsub/setFail")
                .then()
                .statusCode(201);
        LOG.info("Acking was enabled.");

        //assert redelivered message
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> RestAssured.given()
                .get("/google-pubsub/receive/mock/" + GooglePubSubRoutes.ACK_MOCK_RESULT)
                .then()
                .statusCode(200)
                .body(Matchers.is("2")));

        LOG.info("Message \"2\" was delivered after acking was enabled.");
    }

    // This test method may be unnecessary, but there is a problem which is probably linked to the closure of the client.
    // Once https://github.com/apache/camel-quarkus/issues/3957 is solved, this method should be probably removed.
    @Test
    @Order(6)
    public void stopConsumerTest() throws Exception {
        RestAssured.given()
                .config(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
                .get("/google-pubsub/stopConsumer")
                .then()
                .statusCode(204);
    }
}
