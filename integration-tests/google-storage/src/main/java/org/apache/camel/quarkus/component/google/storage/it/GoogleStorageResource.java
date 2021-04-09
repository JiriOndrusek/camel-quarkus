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
package org.apache.camel.quarkus.component.google.storage.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.cloud.storage.Blob;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.jboss.logging.Logger;

@Path("/google-storage")
@ApplicationScoped
public class GoogleStorageResource {

    public static final String PARAM_PORT = "org.apache.camel.quarkus.component.googlr.storage.it.GoogleStorageClientProducer_port";
    public static final String QUERY_PARAM_OBJECT_NAME = "objectName";

    private static final Logger LOG = Logger.getLogger(GoogleStorageResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait("google-storage://my_bucket?operation=getObject", String.class);
        return message;
    }

    @Path("/getObject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getObject(String objectNane) throws Exception {
        final Blob response = producerTemplate.requestBodyAndHeader("google-storage://my_bucket?operation=getObject", null, GoogleCloudStorageConstants.OBJECT_NAME, objectNane, Blob.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(new String(response.getContent()))
                .build();
    }

    @Path("/putObject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putBucket(String body, @QueryParam(QUERY_PARAM_OBJECT_NAME) String objectName) throws Exception {
        final Blob response = producerTemplate.requestBodyAndHeader("google-storage://my_bucket", body, GoogleCloudStorageConstants.OBJECT_NAME, objectName, Blob.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }
}
