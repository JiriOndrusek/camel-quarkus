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
package org.apache.camel.quarkus.component.cics.it;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.redhat.camel.component.cics.CICSConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/cics")
@ApplicationScoped
public class CicsResource {

    private static final Logger LOG = Logger.getLogger(CicsResource.class);

    private static final String COMPONENT_CICS = "cics";

    @ConfigProperty(name = "tcg.tcp.port")
    int tcpPort;

    @ConfigProperty(name = "ctg.host")
    String ctgHost;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/eciReady/COMMAREA/{factory}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response eciReady(Map<String, String> containers,
            @PathParam("factory") String factory) throws Exception {
        String uri = "cics:eci/commarea" + ("noFactory".equals(factory)
                ? "?host=%s&port=%d&protocol=ssl".formatted(ctgHost, tcpPort) : "?gatewayFactory=#" + factory);
        java.nio.file.Path path = Paths.get("target/certs/localhost-truststore.p12");
        uri = uri + "&sslKeyring=" + path.toAbsolutePath() + "&sslPassword=changeit";

        //        System.setProperty("javax.net.ssl.trustStore", path.toAbsolutePath().toString());
        //        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        Exchange ex = producerTemplate.request(uri, e -> {
            e.getIn().setHeader(CICSConstants.CICS_PROGRAM_NAME_HEADER, "ECIREADY");
            e.getIn().setHeader(CICSConstants.CICS_COMM_AREA_SIZE_HEADER, "18");
        });

        Map<String, Object> results = new HashMap<String, Object>(ex.getIn().getHeaders());
        results.put("body", ex.getIn().getBody(String.class));
        return Response.ok(results).build();
    }
}
