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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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

    @ConfigProperty(name = PARAMETER_PORT)
    Integer port;

    @ConfigProperty(name = PARAMETER_HOSTNAME)
    String hostname;

    @ConfigProperty(name = PARAMETER_USERNAME)
    String userbane;

    @ConfigProperty(name = PARAMETER_PASSWORD)
    String password;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait(
                "spring-rabbitmq:cheese?queues=myqueue&routingKey=foo.bar&connectionFactory=#connectionFactory&autoDeclare=true",
                String.class);
        LOG.infof("Received from spring-rabbitmq: %s", message);
        return message;
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String post(String message) throws Exception {
        //        String s = String.format("spring-rabbitmq:%s:%d/foo?connectionFactory=#connectionFactory", hostname, port);
        final String response = producerTemplate.requestBody(
//                "spring-rabbitmq:cheese?routingKey=foo.bar&connectionFactory=#connectionFactory&autoDeclare=true", message,
                "spring-rabbitmq:cheese?routingKey=foo.bar&autoDeclare=true", message,
                String.class);
        LOG.infof("Got response from spring-rabbitmq: %s", response);
        return message;
    }

    @Path("/post2")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String post2(String message) throws Exception {
        //        String s = String.format("spring-rabbitmq:%s:%d/foo?connectionFactory=#connectionFactory", hostname, port);
        final String response = producerTemplate.requestBodyAndHeader(
                "spring-rabbitmq:foo?routingKey=foo.bar&connectionFactory=#connectionFactory", message, "gouda", "cheese",
                String.class);
        LOG.infof("Got response from spring-rabbitmq: %s", response);
        return message;
    }
}
