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
package org.apache.camel.quarkus.component.splunk.it;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/splunk")
@ApplicationScoped
public class SplunkResource {

    public static final String PARAM_REMOTE_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_remotePort";
    public static final String PARAM_TCP_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_tcpPort";
    public static final String SOURCE = "test";
    public static final String SOURCE_TYPE = "testSource";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = PARAM_REMOTE_PORT)
    Integer port;

    @ConfigProperty(name = PARAM_TCP_PORT)
    Integer tcpPort;

    @Inject
    CamelContext camelContext;

    @Path("/normal")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public List normal(String search) throws Exception {
        String url = String.format("splunk://normal?scheme=http&port=%d&delay=5000&initEarliestTime=-10s&search=" + search,
                port);

        final SplunkEvent m1 = consumerTemplate.receiveBody(url, 10000, SplunkEvent.class);
        final SplunkEvent m2 = consumerTemplate.receiveBody(url, 10000, SplunkEvent.class);
        final SplunkEvent m3 = consumerTemplate.receiveBody(url, 10000, SplunkEvent.class);

        List result = Arrays.stream(new SplunkEvent[] { m1, m2, m3 })
                .map(m -> m.getEventData().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith("_"))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1)))
                .collect(Collectors.toList());

        return result;
    }

    @Path("/submit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response submit(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "submit", index, null);
    }

    @Path("/stream")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response stream(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "stream", index, null);
    }

    @Path("/tcp")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tcp(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "tcp", index, tcpPort);
    }

    private Response post(Map<String, String> message, String endpoint, String index, Integer tcpPort) throws Exception {
        SplunkComponent sc = camelContext.getComponent("splunk", SplunkComponent.class);
        sc.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());

        SplunkEvent se = new SplunkEvent();
        for (Map.Entry<String, String> e : message.entrySet()) {
            se.addPair(e.getKey(), e.getValue());
        }

        String url = String.format("splunk:%s?scheme=http&port=%d&index=%s&sourceType=%s&source=%s",
                endpoint, port, index, SOURCE_TYPE, SOURCE);
        if (tcpPort != null) {
            url = url + "&tcpReceiverPort=" + tcpPort;
        }
        final String response = producerTemplate.requestBody(url, se, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
