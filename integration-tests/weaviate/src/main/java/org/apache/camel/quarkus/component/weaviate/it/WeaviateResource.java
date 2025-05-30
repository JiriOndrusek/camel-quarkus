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
package org.apache.camel.quarkus.component.weaviate.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.weaviate.WeaviateVectorDb;
import org.apache.camel.component.weaviate.WeaviateVectorDbAction;
import org.jboss.logging.Logger;

@Path("/weaviate")
@ApplicationScoped
public class WeaviateResource {

    public static final String WEAVIATE_ENDPOINT_URL = "cq.weaviate.endpoint.url";
    public static final String WEAVIATE_ENDPOINT_HOST = "cq.weaviate.endpoint.host";
    public static final String WEAVIATE_ENDPOINT_PORT = "cq.weaviate.endpoint.port";

    private static final Logger LOG = Logger.getLogger(WeaviateResource.class);

    @Inject
    CamelContext context;

    @Path("/createCollection/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponentSaga(@PathParam("name") String name) throws Exception {

        Exchange result = context.createFluentProducerTemplate()
                .to("weaviate:test-collection")
                .withHeader(WeaviateVectorDb.Headers.ACTION, WeaviateVectorDbAction.CREATE_COLLECTION)
                .withHeader(WeaviateVectorDb.Headers.COLLECTION_NAME, name)
                .request(Exchange.class);

        return Response.ok().entity(result.getIn().getBody(String.class)).build();
    }
}
