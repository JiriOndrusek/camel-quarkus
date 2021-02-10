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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SpringRabbitmqTestResource.class)
class SpringRabbitmqTest {

    private final static String EXCHANGE_FOO = "foo";
    private final static String ROUTING_KEY_POLLING = "pollingKey";
    private ConnectionFactory connectionFactory;


    @Test
    public void testInOut() {
        //direct has to be empty
//        RestAssured.get("/spring-rabbitmq/get")
//                .then()
//                .statusCode(204);
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, SpringRabbitmqResource.DIRECT_IN_OUT)
                .queryParam(SpringRabbitmqResource.QUERY_TIMEOUT, 0)
                .post("/spring-rabbitmq/getWait");

        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/spring-rabbitmq/post") //
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, SpringRabbitmqResource.DIRECT_IN_OUT)
                .queryParam(SpringRabbitmqResource.QUERY_TIMEOUT, 0)
                .post("/spring-rabbitmq/getWait")
                .then()
                .statusCode(200)
                .body(is("Hello Sheldon"));

    }

    @Test
    public void testPolling() throws InterruptedException {

        bindQueue(SpringRabbitmqResource.POLLING_QUEUE_NAME, EXCHANGE_FOO, ROUTING_KEY_POLLING);

        //start thread with poling consumer from exchange "foo", polling queueu, routing "mykey", result is sent to polling direct
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_EXCHANGE, EXCHANGE_FOO)
                .queryParam(SpringRabbitmqResource.QUERY_QUEUE, SpringRabbitmqResource.POLLING_QUEUE_NAME)
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, ROUTING_KEY_POLLING)
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, SpringRabbitmqResource.DIRECT_POLLING)
                .post("/spring-rabbitmq/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, ROUTING_KEY_POLLING)
                .body("Sheldon")
                .post("/spring-rabbitmq/send");

        //get result from direct (for pooling) with timeout
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, SpringRabbitmqResource.DIRECT_POLLING)
                .queryParam(SpringRabbitmqResource.QUERY_TIMEOUT, 1000)
                .post("/spring-rabbitmq/getWait")
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

    }

    private void bindQueue(String queue, String exchange, String routingKey) {
        Queue q = new Queue(queue, false);
        DirectExchange t = new DirectExchange(exchange);
        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(q);
        admin.declareExchange(t);
        admin.declareBinding(BindingBuilder.bind(q).to(t).with(routingKey));
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
