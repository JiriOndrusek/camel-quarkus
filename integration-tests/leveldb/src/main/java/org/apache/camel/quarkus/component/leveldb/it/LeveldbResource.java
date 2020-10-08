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
package org.apache.camel.quarkus.component.leveldb.it;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

import static java.util.stream.Collectors.joining;

@Path("/leveldb")
@ApplicationScoped
public class LeveldbResource {

    private static final Logger LOG = Logger.getLogger(LeveldbResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/aggregate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aggregateTest(List<String> messages,
            @QueryParam("path") String path,
            @DefaultValue(LeveldbRouteBuilder.MOCK_RESULT) @QueryParam("mocks") String mockNames) throws Exception {

        String[] mockNamesArray = mockNames.split(",");
        MockEndpoint[] mocks = new MockEndpoint[mockNamesArray.length];

        for (int i = 0; i < mocks.length; i++) {
            mocks[i] = context.getEndpoint(mockNamesArray[i], MockEndpoint.class);
            mocks[i].reset();

            if (i == 0) {
                mocks[i].expectedBodiesReceived(messages.stream().sequential().collect(joining("+")));
            }
        }

        for (String message : messages) {
            producerTemplate.sendBodyAndHeader(path, message, "id", 123);
        }

        mocks[0].assertIsSatisfied(context, 30, TimeUnit.SECONDS);

        Map<String, List<Map<String, Object>>> data = new HashMap();
        for (int i = 0; i < mocks.length; i++) {
            data.put(mockNamesArray[i], extractDataFromMock(mocks[i]));
        }

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(data)
                .build();
    }

    private List<Map<String, Object>> extractDataFromMock(MockEndpoint mockEndpoint) {
        List<Map<String, Object>> data = mockEndpoint.getReceivedExchanges().stream().sequential()
                .map(exchange -> {
                    Map<String, Object> map = new HashMap<>(exchange.getIn().getHeaders());
                    map.put("fromEndpoint", exchange.getFromEndpoint().getEndpointUri());
                    map.put("body", String.valueOf(exchange.getIn().getBody()));
                    return map;
                })
                .collect(Collectors.toList());
        return data;
    }
}
