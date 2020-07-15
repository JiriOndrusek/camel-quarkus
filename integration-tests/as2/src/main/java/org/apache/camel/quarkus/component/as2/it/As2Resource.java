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
package org.apache.camel.quarkus.component.as2.it;

import java.net.URI;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.apache.camel.component.as2.internal.AS2ApiCollection;
import org.apache.camel.component.as2.internal.AS2ClientManagerApiMethod;
import org.apache.http.HttpEntity;
import org.jboss.logging.Logger;

@Path("/as2")
@ApplicationScoped
public class As2Resource {

    private static final Logger LOG = Logger.getLogger(As2Resource.class);
    private static final String PATH_PREFIX = AS2ApiCollection.getCollection().getApiName(AS2ClientManagerApiMethod.class)
            .getName();

    public static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            + "UNH+00000000000117+INVOIC:D:97B:UN'\n"
            + "BGM+380+342459+9'\n"
            + "DTM+3:20060515:102'\n"
            + "RFF+ON:521052'\n"
            + "NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            + "NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            + "CUX+1:USD'\n"
            + "LIN+1++157870:IN'\n"
            + "IMD+F++:::WIDGET'\n"
            + "QTY+47:1020:EA'\n"
            + "ALI+US'\n"
            + "MOA+203:1202.58'\n"
            + "PRI+INV:1.179'\n"
            + "LIN+2++157871:IN'\n"
            + "IMD+F++:::DIFFERENT WIDGET'\n"
            + "QTY+47:20:EA'\n"
            + "ALI+JP'\n"
            + "MOA+203:410'\n"
            + "PRI+INV:20.5'\n"
            + "UNS+S'\n"
            + "MOA+39:2137.58'\n"
            + "ALC+C+ABG'\n"
            + "MOA+8:525'\n"
            + "UNT+23+00000000000117'\n"
            + "UNZ+1+00000000000778'\n";

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ProducerTemplate producerTemplate2;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait("as2:--fix-me--", String.class);
        LOG.infof("Received from as2: %s", message);
        return message;
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Point point) throws Exception {
        LOG.infof("Sending headers to as2: %s", point.getHeaders());
        FluentProducerTemplate pt = producerTemplate.withBody(EDI_MESSAGE);
        Map<String, Object> headers = point.applyHeadersTypeSafe();
        //        if(point.getMessageStructure() != null) {
        //            headers.put(point.getMessageStructureKey(), point.getMessageStructure());
        //        }
        //        if(point.getContentType() != null) {
        //            headers.put(point.getContentTypeKey(), point.getContentType());
        //        }
        //        point.addField("CamelAS2.as2MessageStructure", point.getMessageStructure());
        for (String key : headers.keySet()) {
            pt = pt.withHeader(key, headers.get(key));
        }
        final Object response = pt.toF("as2://client/send?inBody=ediMessage").request(HttpEntity.class);
        Result result = new Result();
        if (response instanceof DispositionNotificationMultipartReportEntity) {
            result.setDispositionNotificationMultipartReportEntity(true);
            result.setPartsCount(((DispositionNotificationMultipartReportEntity) response).getPartCount());
        }

        LOG.infof("Got response from as2: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(result)
                .build();
    }
}
