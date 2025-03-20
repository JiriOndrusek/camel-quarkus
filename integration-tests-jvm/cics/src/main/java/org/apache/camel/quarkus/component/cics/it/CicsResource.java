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

import java.util.Map;

import com.redhat.camel.component.cics.CICSConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/cics")
@ApplicationScoped
public class CicsResource {

    private static final Logger LOG = Logger.getLogger(CicsResource.class);

    private static final String COMPONENT_CICS = "cics";
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/test")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponentSap() throws Exception {
        Exchange ex = producerTemplate.request("direct:test", e -> {
            e.getIn().setHeader(CICSConstants.CICS_PROGRAM_NAME_HEADER, "ECIREADY");
            e.getIn().setHeader(CICSConstants.CICS_COMM_AREA_SIZE_HEADER, "18");
        });

        Object o = producerTemplate.requestBodyAndHeaders("direct:test",
                Map.of(CICSConstants.CICS_PROGRAM_NAME_HEADER, "ECIREADY",
                        CICSConstants.CICS_COMM_AREA_SIZE_HEADER, "18"),
                null);

        return null;
    }
}
