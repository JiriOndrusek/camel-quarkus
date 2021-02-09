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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SpringRabbitmqTestResource.class)
class SpringRabbitmqTest {

    @Test
    public void testInOut() {
        //direct has to be empty
        RestAssured.get("/spring-rabbitmq/get")
                .then()
                .statusCode(204);

        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/spring-rabbitmq/post") //
                .then()
                .statusCode(204);

        RestAssured.get("/spring-rabbitmq/get")
                .then()
                .statusCode(200)
                .body(is("Hello Sheldon"));

    }
    @Test
    public void testPolling() throws InterruptedException {
        String hostname = System.getProperty(SpringRabbitmqResource.PARAMETER_HOSTNAME);
        String port = System.getProperty(SpringRabbitmqResource.PARAMETER_PORT);
        String usernane = System.getProperty(SpringRabbitmqResource.PARAMETER_USERNAME);
        String password = System.getProperty(SpringRabbitmqResource.PARAMETER_PASSWORD);


        CachingConnectionFactory cf = new CachingConnectionFactory();
        cf.setUri(String.format("amqp://%s:%s", hostname, port));
        cf.setUsername(usernane);
        cf.setPassword(password);

        Queue q = new Queue("myqueue", false);
        DirectExchange t = new DirectExchange("foo");
        AmqpAdmin admin = new RabbitAdmin(cf);
        admin.declareQueue(q);
        admin.declareExchange(t);
        admin.declareBinding(BindingBuilder.bind(q).to(t).with("mykey"));


//
//
//        Queue q = new Queue("myqueue");
//        TopicExchange t = new TopicExchange("cheese");
//
//        AmqpAdmin admin = new RabbitAdmin(cf);
//        admin.declareQueue(q);
//        admin.declareExchange(t);
//        admin.declareBinding(BindingBuilder.bind(q).to(t).with("foo.bar.#"));
        
        //direct has to be empty
        RestAssured.get("/spring-rabbitmq/get2")
                .then()
                .statusCode(204);

//        RestAssured.given() //
//                .contentType(ContentType.TEXT)
//                .body("Sheldon")
//                .post("/spring-rabbitmq/post") //
//                .then()
//                .statusCode(204);

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/spring-rabbitmq/start");

        Thread.sleep(1000);

        RestAssured.get("/spring-rabbitmq/get")
                .then()
                .statusCode(204)
                .body(is("Hello Sheldon"));

    }

//    @Test
//    public void testInOut() throws Exception {

        //       //wait for the result from the server
        //       await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
        //           String result = RestAssured.get("/spring-rabbitmq/get")
        //                   .then()
        //                   .extract().asString();
        ////           Assertions.assertEquals("Hello", result);
        //       });

//        RestAssured.given() //
//                .contentType(ContentType.TEXT)
//                .body("Hello")
//                .post("/spring-rabbitmq/post") //
//                .then()
//                .statusCode(201);
//
//    }

    @Test
    public void testProducerWithHeader() throws Exception {
        //        ConnectionFactory cf = context.getRegistry().lookupByNameAndType("myCF", ConnectionFactory.class);
        //
        //        Queue q = new Queue("myqueue");
        //        TopicExchange t = new TopicExchange("foo");
        //
        //        AmqpAdmin admin = new RabbitAdmin(cf);
        //        admin.declareQueue(q);
        //        admin.declareExchange(t);
        //        admin.declareBinding(BindingBuilder.bind(q).to(t).with("foo.bar.#"));

        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body("Hello")
                .post("/spring-rabbitmq/post");

        //        AmqpTemplate template = new RabbitTemplate(cf);
        //        Message out = template.receive("myqueue");
        //        Assertions.assertEquals("Hello World", new String(out.getBody()));
        //        Assertions.assertEquals("gouda", out.getMessageProperties().getHeader("cheese"));
    }

}
