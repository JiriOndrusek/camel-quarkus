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
package org.apache.camel.quarkus.component.aws2.cw.it;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.scheduler.Scheduled;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.cw.Cw2Constants;

@Path("/aws2-cw")
@ApplicationScoped
public class Aws2CwResource {

    @Inject
    ProducerTemplate producerTemplate;

    private volatile String endpointUri;

    @Scheduled(every = "1s")
    void schedule() {
        if (endpointUri != null) {
            producerTemplate.requestBody(endpointUri, null, String.class);
        }
    }

    @Path("/send-metric/{namespace}/{metric-name}/{metric-unit}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            String value,
            @PathParam("namespace") String namespace,
            @PathParam("metric-name") String name,
            @PathParam("metric-unit") String unit) throws Exception {
        endpointUri = "aws2-cw://" + namespace + "?name=" + name + "&value=" + value + "&unit=" + unit;
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/send-metric-maps/{namespace}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postMap(
            String data,
            @PathParam("namespace") String namespace,
            @QueryParam("customClientName") String customClientName) throws Exception {

        String uri = "aws2-cw://" + namespace;
        uri = customClientName != null && !customClientName.isEmpty()
                ? uri + "?autowiredEnabled=false&amazonCwClient=#" + customClientName : uri;


        String[] items = data.split(";;");



        for (String s : items) {

            Map<String, Object> headers = new HashMap<>();

            String[] item = s.split(";");
            for(int i = 0; i + 1 < item.length;) {

                String key = item[i];
                String stringValue = item[i + 1];
                Object value;
                if(Cw2Constants.METRIC_VALUE.equals(key)) {
                    value = Integer.parseInt(stringValue);
                } else if(Cw2Constants.METRIC_DIMENSIONS.equals(key)) {
                    Map<String, String> m = new HashMap<>();
                    m.put()
                    value = item[i + 1];
                }

                headers.put(key, value);
                i += 2;
            }
//            //use Instant as timestamp header
//            Map<String, Object> typedHeaders = item.entrySet().stream().collect(Collectors.toMap(
//                    e -> e.getKey(),
//                    e -> {
//                        if (Cw2Constants.METRIC_TIMESTAMP.equals(e.getKey()) && e.getValue() instanceof Long) {
//                            return Instant.ofEpochMilli((Long) e.getValue());
//                        }
//                        return e.getValue();
//                    }));
            try {
                producerTemplate.requestBodyAndHeaders(uri, null, headers, String.class);
            } catch (Exception e) {
                if (e instanceof CamelExecutionException && e.getCause() != null) {
                    return Response
                            .ok(e.getCause().getMessage())
                            .build();
                }
                throw e;
            }
        }

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }
}
