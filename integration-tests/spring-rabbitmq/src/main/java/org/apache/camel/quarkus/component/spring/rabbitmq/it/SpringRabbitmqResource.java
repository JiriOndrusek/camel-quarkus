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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.concurrent.Executors;

@Path("/spring-rabbitmq")
@ApplicationScoped
public class SpringRabbitmqResource {
    private static final Logger LOG = Logger.getLogger(SpringRabbitmqResource.class);

    public static final String PARAMETER_PORT = "camel.quarkus.spring-rabbitmq.test.port";
    public static final String PARAMETER_HOSTNAME = "camel.quarkus.spring-rabbitmq.test.hostname";
    public static final String PARAMETER_USERNAME = "camel.quarkus.spring-rabbitmq.test.username";
    public static final String PARAMETER_PASSWORD = "camel.quarkus.spring-rabbitmq.test.password";
    
    public static final String URL_PARAMETER_TOPIC = "topic";
    public static final String URL_PARAMETER_ROUTING_KEY = "routingKey";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_PORT)
    Integer port;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_HOSTNAME)
    String hostname;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_USERNAME)
    String usernane;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_PASSWORD)
    String password;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait(
                "direct:result",
                String.class);
        return message;
    }

    @Path("/getWait")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getWait() throws Exception {
        final String message = consumerTemplate.receiveBody(
                "direct:result2",
                1000,
                String.class);
        return message;
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void post(String message) throws Exception {
        final String response = producerTemplate.requestBody(
                                "spring-rabbitmq:cheese?routingKey=foo.bar", message,
                String.class);
    }

    @Path("/start")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void start(String message) throws Exception {
        producerTemplate.sendBody("direct:start", message);
    }

    @Path("/startPolling")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String startPolling() throws Exception {
//        CachingConnectionFactory cf = new CachingConnectionFactory();
//        cf.setUri(String.format("amqp://%s:%d", hostname, port));
//        cf.setUsername(usernane);
//        cf.setPassword(password);
//
//        Queue q = new Queue("myqueue");
//        TopicExchange t = new TopicExchange("foo");
//
//        AmqpAdmin admin = new RabbitAdmin(cf);
//        admin.declareQueue(q);
//        admin.declareExchange(t);
//        admin.declareBinding(BindingBuilder.bind(q).to(t).with("foo.bar.#"));

//        final String message = consumerTemplate.receiveBodyNoWait(
//                "spring-rabbitmq:cheese?queues=myqueue&connectionFactory=#connectionFactory&autoDeclare=true",
//                String.class);
//        LOG.infof("Received from spring-rabbitmq: %s", message);
//        return message;

        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String body = consumerTemplate.receiveBody("spring-rabbitmq:foo?queues=myqueue&routingKey=mykey", String.class);
            System.out.println("received " + body);
            producerTemplate.sendBody("spring-rabbitmq:foo?routingKey=mykey2", "Polling Hello " + body);
        });



        return null;
    }
}
