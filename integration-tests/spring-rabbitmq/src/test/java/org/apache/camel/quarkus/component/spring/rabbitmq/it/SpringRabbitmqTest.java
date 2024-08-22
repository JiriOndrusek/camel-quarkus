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
package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@WithTestResource(SpringRabbitmqTestResource.class)
class SpringRabbitmqTest {

    private final static String EXCHANGE_POLLING = "polling";
    private final static String ROUTING_KEY_POLLING = "pollingKey";
    private ConnectionFactory connectionFactory;

    @Test
    public void testDefault() {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-default", "any_exchange", "any_key");
        //send a message using default keyword, so the routingKey will be used as queue
        sendToExchange("default", "queue-for-default", "content for default test");

        //read from "queueForDefault" using default exchange name without routingKey
        RestAssured.given()
                .queryParam("exchange", "default")
                .queryParam("queue", "queue-for-default")
                .post("/spring-rabbitmq/consume")
                .then()
                .statusCode(200)
                .body(is("content for default test"));
    }

    @Test
    public void testAutoDeclare() {
        //send msg which should be routed into directs direct:autoDeclare1 and direct:autoDeclare2
        RestAssured.given()
                .queryParam("exchange", "exchange-for-autoDeclare")
                .queryParam("routingKey", "routing-key-for-autoDeclare")
                .body("content for autoDeclare test")
                .post("/spring-rabbitmq/send").then().statusCode(200);

        //direct:autoDeclare1 is defined without autoDeclare
        getFromDirect("direct:autoDeclare1")
                .then()
                .statusCode(200)
                //todo better validation
                .body(is(""));

        getFromDirect("direct:autoDeclare2")
                .then()
                .statusCode(200)
                .body(is("Hello from auto-declared2: content for autoDeclare test"));
    }

    @Test
    public void testHeadersToProperties() throws Exception {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-headersToProperties", "exchange-for-headersToProperties", "key-for-headersToProperties");

        String headers = SpringRabbitmqUtil
                .headersToString(Map.of(SpringRabbitMQConstants.DELIVERY_MODE, MessageDeliveryMode.PERSISTENT,
                        SpringRabbitMQConstants.TYPE, "price",
                        SpringRabbitMQConstants.CONTENT_TYPE, "application/xml",
                        SpringRabbitMQConstants.MESSAGE_ID, "0fe9c142-f9c1-426f-9237-f5a4c988a8ae",
                        SpringRabbitMQConstants.PRIORITY, 1));

        RestAssured.given()
                .queryParam("exchange", "exchange-for-headersToProperties")
                .queryParam("routingKey", "key-for-headersToProperties")
                .queryParam("headers", headers)
                .queryParam("componentName", "customHeaderFilterStrategySpringRabbit")
                .body("<price>123</price>")
                .post("/spring-rabbitmq/send").then().statusCode(200);

        AmqpTemplate template = new RabbitTemplate(connectionFactory);
        Message out = template.receive("queue-for-headersToProperties");

        final MessageProperties messageProperties = out.getMessageProperties();
        Assertions.assertNotNull(messageProperties, "The message properties should not be null");
        String encoding = messageProperties.getContentEncoding();
        assertThat(Charset.defaultCharset().name()).isEqualTo(encoding);
        assertThat(new String(out.getBody(), encoding)).isEqualTo("<price>123</price>");
        assertThat(messageProperties.getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(messageProperties.getType()).isEqualTo("price");
        assertThat(messageProperties.getContentType()).isEqualTo("application/xml");
        assertThat(messageProperties.getMessageId()).isEqualTo("0fe9c142-f9c1-426f-9237-f5a4c988a8ae");
        assertThat(messageProperties.getPriority()).isEqualTo(1);
        //the only headers preserved by customHeadersFilterStrategy is "CamelSpringRabbitmqMessageId
        assertThat(messageProperties.getHeaders().size()).isEqualTo(1);
        assertThat(messageProperties.getHeaders()).containsKey("CamelSpringRabbitmqMessageId");
    }

    @Test
    public void testReuse() {
        //send msg without reuse
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(200);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse1 for key1: Hello"));

        //overriding exchange
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .queryParam("headers",
                        SpringRabbitmqUtil
                                .headersToString(Map.of(SpringRabbitMQConstants.EXCHANGE_OVERRIDE_NAME, "exchange-for-reuse2")))
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(200);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse2 for key1: Hello"));

        //overriding exchange and key
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .queryParam("headers",
                        SpringRabbitmqUtil
                                .headersToString(Map.of(SpringRabbitMQConstants.EXCHANGE_OVERRIDE_NAME, "exchange-for-reuse2",
                                        SpringRabbitMQConstants.ROUTING_OVERRIDE_KEY, "key-for-reuse2")))
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(200);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse2 for key2: Hello"));
    }

    @Test
    public void testManualAcknowledgement() {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-manual-ack", "exchange-for-manual-ack", "key-for-manual-ack");
        //send message with 20 seconds processing time
        RestAssured.given()
                .queryParam("exchange", "exchange-for-manual-ack")
                .queryParam("routingKey", "key-for-manual-ack")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(200);

        //message is not acked in rabbitmq (in 5 seconds)
        getFromDirect("direct:manual-ack")
                .then()
                .statusCode(200)
                .body(is(""));

        //should be acked in 20 seconds
        Awaitility.await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = getFromDirect("direct:manual-ack");

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().asString()).isEqualTo("Processed: Hello");
        });
    }

    @Test
    public void testDMLC() {
        int count = 20;
        //sent 20 messages
        for (int i = 0; i < count; i++) {

            RestAssured.given()
                    .queryParam("exchange", "exchange-for-dmlc")
                    .queryParam("routingKey", "key-for-dmlc")
                    .body("Hello" + i)
                    .post("/spring-rabbitmq/send").then().statusCode(200);
        }

        //read results
        List<Object> results = SpringRabbitmqUtil.stringToList(RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, "direct:dmlc")
                .queryParam("numberOfMessages", 20)
                .queryParam("cacheResults", true)
                .post("/spring-rabbitmq/getFromDirect")
                .then().statusCode(200)
                .extract().body().asString());

        assertThat(results).hasSize(count + 1);
        //the whole duration has to be longer than count
        // (part of the first second is taken for the calls to resource, so the duration has to be >= count-1)
        assertThat(Integer.parseInt((String) results.get(0))).isLessThan(count - 1);
        for (int i = 0; i < count; i++) {
            assertThat(results).contains("Hello from DMLC: Hello" + i);
        }
    }

    @Test
    public void testSMLC() {
        int count = 20;
        //sent 20 messages
        for (int i = 0; i < count; i++) {

            RestAssured.given()
                    .queryParam("exchange", "exchange-for-smlc")
                    .queryParam("routingKey", "key-for-smlc")
                    .body("Hello" + i)
                    .post("/spring-rabbitmq/send").then().statusCode(200);
        }

        //read results
        List<Object> results = SpringRabbitmqUtil.stringToList(RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, "direct:smlc")
                .queryParam("numberOfMessages", 20)
                .queryParam("cacheResults", true)
                .post("/spring-rabbitmq/getFromDirect")
                .then().statusCode(200)
                .extract().body().asString());

        assertThat(results).hasSize(count + 1);
        //the whole duration has to be longer than count
        // (part of the first second is taken for the calls to resource, so the duration has to be >= count-1)
        assertThat(Integer.parseInt((String) results.get(0))).isGreaterThanOrEqualTo(count - 1);
        for (int i = 0; i < count; i++) {
            assertThat(results).contains("Hello from SMLC: Hello" + i);
        }
    }

    @Test
    public void testPolling() throws InterruptedException {

        bindQueue(SpringRabbitmqResource.POLLING_QUEUE_NAME, EXCHANGE_POLLING, ROUTING_KEY_POLLING);

        //start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_EXCHANGE, EXCHANGE_POLLING)
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, ROUTING_KEY_POLLING)
                .post("/spring-rabbitmq/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        sendToExchange(EXCHANGE_POLLING, ROUTING_KEY_POLLING, "Sheldon");

        //get result from direct (for polling) with timeout
        getFromDirect(SpringRabbitmqResource.DIRECT_POLLING)
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

    }

    @Test
    public void testFanOut() {
        bindQueue("queue-for-fanout-A", "exchange-for-fanout", "key-for-fanout-A");
        bindQueue("queue-for-fanout-B", "exchange-for-fanout", "key-for-fanout-B");
        //send message without key to fanout exchange
        RestAssured.given()
                .queryParam("exchange", "exchange-for-fanout")
                .queryParam("exchangeType", "fanout")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(200);

        //message is not acked in rabbitmq (in 5 seconds)
        getFromDirect("direct:manual-ack")
                .then()
                .statusCode(200)
                .body(is(""));

        //should be acked in 20 seconds
        Awaitility.await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = getFromDirect("direct:manual-ack");

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().asString()).isEqualTo("Processed: Hello");
        });

        AmqpTemplate template = new RabbitTemplate(connectionFactory);
        Message outA = template.receive("queue-for-fanout-A");
        Message outB = template.receive("queue-for-fanout-B");
        System.out.println(outA.getBody());
    }

    private void sendToExchange(String exchange, String routingKey, String body) {
        RequestSpecification rs = RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_EXCHANGE, exchange)
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, routingKey)
                .body(body);

        rs.post("/spring-rabbitmq/send");
    }

    private Response getFromDirect(String direct) {
        return RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, direct)
                .post("/spring-rabbitmq/getFromDirect");
    }

    private void bindQueue(String queue, String exchange, String routingKey) {
        Queue q = new Queue(queue, false);

        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(q);
        DirectExchange t = new DirectExchange(exchange);
        admin.declareExchange(t);
        admin.declareBinding(BindingBuilder.bind(q).to(t).with(routingKey));
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
