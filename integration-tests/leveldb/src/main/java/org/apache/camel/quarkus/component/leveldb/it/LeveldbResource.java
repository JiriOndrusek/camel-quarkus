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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

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
    public Response aggregateTest(List<String> messages, @QueryParam("path") String path) throws Exception {
        MockEndpoint mock = context.getEndpoint(LeveldbRouteBuilder.MOCK_RESULT, MockEndpoint.class);
        mock.reset();
        mock.expectedBodiesReceived("SHELDON");

        for (String message : messages) {
            producerTemplate.sendBodyAndHeader(path, message, "id", 123);
        }

        mock.assertIsSatisfied(context, 30, TimeUnit.SECONDS);

        Map<String, Object> headers = mock.getReceivedExchanges().get(0).getIn().getHeaders();
        headers.put("fromEndpoint", mock.getReceivedExchanges().get(0).getFromEndpoint().getEndpointUri());

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(headers)
                .build();
    }
}
