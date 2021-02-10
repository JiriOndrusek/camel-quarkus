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

import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/spring-rabbitmq")
@ApplicationScoped
public class SpringRabbitmqResource {
    private static final Logger LOG = Logger.getLogger(SpringRabbitmqResource.class);

    public static final String PARAMETER_PORT = "camel.quarkus.spring-rabbitmq.test.port";
    public static final String PARAMETER_HOSTNAME = "camel.quarkus.spring-rabbitmq.test.hostname";
    public static final String PARAMETER_USERNAME = "camel.quarkus.spring-rabbitmq.test.username";
    public static final String PARAMETER_PASSWORD = "camel.quarkus.spring-rabbitmq.test.password";

    public static final String IN_OUT_QUEUE_NAME = "inOutQueue";
    public static final String POLLING_QUEUE_NAME = "pollingqueue";

    public static final String DIRECT_IN_OUT = "direct:inOut";
    public static final String DIRECT_POLLING = "direct:polling";

    public static final String QUERY_ROUTING_KEY = "routingKey";
    public static final String QUERY_DIRECT = "fromDirect";
    public static final String QUERY_EXCHANGE = "exchange";
    public static final String QUERY_QUEUE = "queue";
    public static final String QUERY_TIMEOUT = "timeout";

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
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getWait(@QueryParam(QUERY_DIRECT) String direct, @QueryParam(QUERY_TIMEOUT) int timeout) {
        final String message = timeout > 0 ?
                consumerTemplate.receiveBody(direct, timeout, String.class) :
                consumerTemplate.receiveBodyNoWait(direct, String.class);
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

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void send(String message, @QueryParam(QUERY_ROUTING_KEY) String routingKey) {
        String url = String.format("spring-rabbitmq:foo?routingKey=%s&autoDeclare=true", routingKey);
        producerTemplate.sendBody(url, message);
    }

    @Path("/startPolling")
    @POST
    public void startPolling(@QueryParam(QUERY_EXCHANGE) String exchange,
                             @QueryParam(QUERY_ROUTING_KEY) String routingKey,
                             @QueryParam(QUERY_QUEUE) String queue,
                             @QueryParam(QUERY_DIRECT) String direct) throws Exception {
        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String url = String.format("spring-rabbitmq:%s?queues=%s&routingKey=%s", exchange, queue, routingKey);
            String body = consumerTemplate.receiveBody(url, String.class);
            producerTemplate.sendBody(direct, "Polling Hello " + body);
        });
    }
}
