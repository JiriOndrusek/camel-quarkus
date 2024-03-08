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
package org.apache.camel.quarkus.component.jt400.it;

import com.ibm.as400.access.MockAS400ImplRemote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jt400.Jt400Endpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/jt400")
@ApplicationScoped
public class Jt400Resource {

    @ConfigProperty(name = "cq.jt400.url")
    String jt400Url;

    @ConfigProperty(name = "cq.jt400.username")
    String jt400USername;

    @ConfigProperty(name = "cq.jt400.password")
    String jt400Password;

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    MockAS400ImplRemote as400ImplRemote;

    @Path("/keyedDataQueue/read/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response keyedDataQueueRead(@PathParam("key") String key) {

        String url = String.format(
                "jt400://%s:%s@%s/QSYS.LIB/QGPL.LIB/SAMPLEQ.DTAQ?keyed=true&format=binary&searchKey=%s&searchType=GE", jt400USername,
                jt400Password, jt400Url, key);

        Exchange ex = consumerTemplate.receive(url);

        return Response.ok().entity(ex.getIn().getBody(String.class)).build();
    }

    @Path("/keyedDataQueue/write/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response keyedDataQueueWrite(@PathParam("key") String key, String data) throws Exception {
        String url = getBaseUrl() + "/QSYS.LIB/QGPL.LIB/SAMPLEQ.DTAQ?keyed=true";

        Object ex = producerTemplate.requestBodyAndHeader(
                url,
                data,
                Jt400Endpoint.KEY,
                key);
        return Response.ok().entity(ex).build();
    }
    //
    //    @Path("/messageQueue/read")
    //    @GET
    //    @Produces(MediaType.TEXT_PLAIN)
    //    public Response messageQueueRead() throws InterruptedException {
    //        Exchange ex = consumerTemplate.receive(
    //                "jt400://username:password@system/qsys.lib/MSGOUTQ.MSGQ?connectionPool=#mockPool&readTimeout=100");
    //        if (ex.getIn().getBody() != null) {
    //            //reurn ok,because something is returned (the message contains 1 char, which is not correctly converted)
    //            return Response.ok().build();
    //        }
    //
    //        return Response.serverError().build();
    //    }
    //
    //    @Path("/messageQueue/write/{key}")
    //    @POST
    //    @Consumes(MediaType.TEXT_PLAIN)
    //    @Produces(MediaType.TEXT_PLAIN)
    //    public Response messageQueueWrite(@PathParam("key") String key, String data) throws Exception {
    //
    //        Object ex = producerTemplate.requestBodyAndHeader(
    //                "jt400://username:password@system/qsys.lib/MSGINQ.MSGQ?connectionPool=#mockPool",
    //                data,
    //                Jt400Endpoint.KEY,
    //                key);
    //        return Response.ok().entity(ex).build();
    //    }
    //
        @Path("/programCall")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_JSON)
        public Response programCall() throws Exception {
            String url = getBaseUrl() + "/QSYS.LIB/assets.LIB/compute.PGM?connectionPool=#mockPool&outputFieldsIdx=1&fieldsLength=10,10,512";
            Object ex = producerTemplate.requestBody(
                    url,
                    new String[] { "par1", "par2" });
            return Response.ok().entity(ex).build();
        }
    //
    //    @Path("/put/mockResponse")
    //    @POST
    //    @Consumes(MediaType.APPLICATION_JSON)
    //    public Response putMockResponse(
    //            Map params) throws Exception {
    //        DataStream dataStream = switch (ReplyType.valueOf((String) params.get("replyType"))) {
    //        case DQReadNormal -> new ReplyDQReadNormal((Integer) params.get("hashCode"),
    //                (String) params.get("senderInformation"),
    //                (String) params.get("entry"),
    //                (String) params.get("key"));
    //        case ok -> new ReplyOk();
    //        case DQCommonReply -> new ReplyDQCommon(
    //                (Integer) params.get("hashCode"));
    //        case DQRequestAttributesNormal -> new ReplyDQRequestAttributesNormal(
    //                (Integer) params.get("keyLength"));
    //        case RCExchangeAttributesReply -> new ReplyRCExchangeAttributes();
    //        case RCCallProgramReply -> new ReplyRCCallProgram();
    //        };
    //
    //        MockedResponses.add(dataStream);
    //
    //        return Response.ok().build();
    //    }


    private String getBaseUrl() {
        return String.format("jt400://%s:%s@%s", jt400USername, jt400Password, jt400Url);
    }
}
