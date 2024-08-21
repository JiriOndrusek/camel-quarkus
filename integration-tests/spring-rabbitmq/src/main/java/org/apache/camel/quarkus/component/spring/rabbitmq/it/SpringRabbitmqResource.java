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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.jboss.logging.Logger;

@Path("/spring-rabbitmq")
@ApplicationScoped
public class SpringRabbitmqResource {
    private static final Logger LOG = Logger.getLogger(SpringRabbitmqResource.class);

    public static final String PARAMETER_PORT = "camel.quarkus.spring-rabbitmq.test.port";
    public static final String PARAMETER_HOSTNAME = "camel.quarkus.spring-rabbitmq.test.hostname";
    public static final String PARAMETER_USERNAME = "camel.quarkus.spring-rabbitmq.test.username";
    public static final String PARAMETER_PASSWORD = "camel.quarkus.spring-rabbitmq.test.password";

    public static final String ROUTING_KEY_IN_OUT = "foo.bar";
    public static final String EXCHANGE_IN_OUT = "inOut";

    public static final String POLLING_QUEUE_NAME = "pollingQueue";

    public static final String DIRECT_IN_OUT = "direct:inOut";
    public static final String DIRECT_POLLING = "direct:polling";

    public static final String QUERY_ROUTING_KEY = "routingKey";
    public static final String QUERY_DIRECT = "fromDirect";
    public static final String QUERY_EXCHANGE = "exchange";
    public static final String QUERY_TIMEOUT = "timeout";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    Map<String, List<String>> resultsMap;

    @Path("/consume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response consume(@QueryParam("queue") String queue,
            @QueryParam("exchange") String exchange,
            @QueryParam("routingKey") String routingKey,
            @QueryParam("autoDeclare") String autoDeclare) {
        String url = "spring-rabbitmq:" + exchange + "?connectionFactory=#connectionFactory";
        if (routingKey != null) {
            url += "&routingKey=" + routingKey;
        }
        if (queue != null) {
            url += "&queues=" + queue;
        }
        if (autoDeclare != null) {
            url += "&autoDeclare=" + autoDeclare;
        }
        try {
            return Response.ok().entity(consumerTemplate.receiveBody(url, 5000, String.class)).build();
        } catch (RuntimeCamelException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return Response.status(500).entity(sw.toString()).build();
        }
    }

    @Path("/getFromDirect")
    @POST
    public Response getFromDirect(@QueryParam(QUERY_DIRECT) String directName,
            @QueryParam("timeout") Long timeout,
            @QueryParam("numberOfMessages") Integer numberOfMessages) {
        try {
            long _timeout = timeout != null ? timeout : 5000;
            if (numberOfMessages != null) {
                Instant start = Instant.now();
                LinkedList<String> results = new LinkedList<>();
                for (int i = 0; i < numberOfMessages; i++) {
                    String msg = consumerTemplate.receiveBody(directName, _timeout, String.class);
                    if (msg == null || msg.isEmpty()) {
                        break;
                    }
//                    Log.infof("Message: %s", msg);
                    results.add(msg);
                }

                Duration timeElapsed = Duration.between(start, Instant.now());
                results.addFirst(timeElapsed.getSeconds() + "");

                return Response.ok(SpringRabbitmqUtil.listToString(results)).build();
            }

            return Response.ok(consumerTemplate.receiveBody(directName, _timeout, String.class)).build();
        } catch (RuntimeCamelException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return Response.status(500).entity(sw.toString()).build();
        }
    }

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response send(String message,
            @QueryParam("headers") String headers,
            @QueryParam(QUERY_EXCHANGE) String exchange,
            @QueryParam(QUERY_ROUTING_KEY) String routingKey,
            @QueryParam("componentName") String componentName) {
        String url = String.format(
                "%s:%s?connectionFactory=#connectionFactory&routingKey=%s",
                componentName == null ? "spring-rabbitmq" : componentName, exchange, routingKey);
        try {
            if (headers != null) {
                producerTemplate.sendBodyAndHeaders(url, message, SpringRabbitmqUtil.stringToHeaders(headers));
            } else {
                producerTemplate.sendBody(url, message);
            }
            return Response.ok().build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return Response.status(500).entity(sw.toString()).build();
        }
    }

    @Path("/startPolling")
    @POST
    public void startPolling(@QueryParam(QUERY_EXCHANGE) String exchange,
            @QueryParam(QUERY_ROUTING_KEY) String routingKey) {
        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String url = String.format("spring-rabbitmq:%s?queues=%s&routingKey=%s", exchange, POLLING_QUEUE_NAME, routingKey);
            String body = consumerTemplate.receiveBody(url, String.class);
            producerTemplate.sendBody(DIRECT_POLLING, "Polling Hello " + body);
        });
    }

    private void addMsgToResultsMap(String queue, String msg) {
        List<String> results = resultsMap.get(queue);
        if (results == null) {
            results = new LinkedList<>();
            resultsMap.put(queue, results);
        }
        results.add(msg);
        Log.infof("Received message %s", msg);
    }
}
