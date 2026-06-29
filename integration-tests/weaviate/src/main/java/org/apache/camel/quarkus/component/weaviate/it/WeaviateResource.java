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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.weaviate.client6.v1.api.collections.WeaviateObject;
import io.weaviate.client6.v1.api.collections.query.QueryResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.jboss.logging.Logger;

@Path("/weaviate")
@ApplicationScoped
public class WeaviateResource {

    public static final String WEAVIATE_CONTAINER_ADDRESS = "cq.weaviate.container.address";
    public static final String WEAVIATE_CONTAINER_GRPC_PORT = "cq.weaviate.container.grpc.port";
    public static final String WEAVIATE_API_KEY_ENV = "WEAVIATE_API_KEY";
    public static final String WEAVIATE_HOST_ENV = "WEAVIATE_HOST";

    private static final Logger LOG = Logger.getLogger(WeaviateResource.class);

    @Inject
    CamelContext context;

    @Path("/request")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response request(Map<String, Object> testHeaders) {
        Map<String, Object> headers = new HashMap<>(testHeaders);

        Object body = headers.get("body");
        headers.remove("body");

        //convert Double to Float in body list (for create)
        if (body instanceof List) {
            body = ((List<?>) body).stream().map(o -> o instanceof Double ? ((Double) o).floatValue() : o)
                    .collect(Collectors.toList());
        }

        Exchange response = context.createFluentProducerTemplate()
                .to("direct:weaviate")
                .withBody(body)
                .withHeaders(headers)
                .request(Exchange.class);

        if (response.getException() != null) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("error", response.getException().getMessage());
            return Response.ok(map).build();
        }

        Object result = response.getIn().getBody();
        LOG.debugf("Response for collections with headers (%s) is: \"%s\".", headers, result);

        if (result != null) {
            HashMap<String, Object> map = new HashMap<>();

            if (result instanceof Boolean) {
                map.put("result", result);
            } else if (result instanceof WeaviateObject) {
                @SuppressWarnings("unchecked")
                WeaviateObject<Map<String, Object>> wo = (WeaviateObject<Map<String, Object>>) result;
                map.put("result", wo.uuid());
                map.put("resultProperties", wo.properties());
            } else if (result instanceof Optional) {
                @SuppressWarnings("unchecked")
                Optional<WeaviateObject<Map<String, Object>>> opt = (Optional<WeaviateObject<Map<String, Object>>>) result;
                if (opt.isPresent()) {
                    WeaviateObject<Map<String, Object>> wo = opt.get();
                    map.put("result", Map.of(wo.uuid(), wo.properties()));
                }
            } else if (result instanceof QueryResponse) {
                @SuppressWarnings("unchecked")
                QueryResponse<Map<String, Object>> qr = (QueryResponse<Map<String, Object>>) result;
                List<Map<String, Object>> objects = qr.objects().stream()
                        .map(obj -> {
                            Map<String, Object> m = new HashMap<>(obj.properties());
                            m.put("_uuid", obj.uuid());
                            return m;
                        })
                        .collect(Collectors.toList());
                map.put("result", objects);
            }

            return Response.ok(map).build();
        }

        return Response.status(500).entity("Empty result").build();
    }

}
