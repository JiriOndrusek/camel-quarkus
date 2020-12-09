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
package org.apache.camel.quarkus.component.minio.it;

import java.net.URI;

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
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/minio")
@ApplicationScoped
public class MinioResource {

    private static final Logger LOG = Logger.getLogger(MinioResource.class);

    public static final String SERVER_ACCESS_KEY = "testAccessKey";
    public static final String SERVER_SECRET_KEY = "testSecretKey";
    public static final String PARAM_SERVER_HOST = MinioResource.class.getSimpleName() + "_serverHost";
    public static final String PARAM_SERVER_PORT = MinioResource.class.getSimpleName() + "_serverPort";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait("minio:--fix-me--", String.class);
        LOG.infof("Received from minio: %s", message);
        return message;
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        LOG.infof("Sending to minio: %s", message);
        final String response = producerTemplate.requestBody("minio:--fix-me--", message, String.class);
        LOG.infof("Got response from minio: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }


    @Path("/consumer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumer() {

        String serverUrl = "http://" +System.getProperty(PARAM_SERVER_HOST) + ":" + System.getProperty(PARAM_SERVER_PORT);

        final String message = consumerTemplate.receiveBody("minio://mycamel?moveAfterRead=true&destinationBucketName=camel-kafka-connector&autoCreateBucket=true"
                + "&accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")"
                + "&endpoint=" + serverUrl, 5000, String.class);
        LOG.infof("Received from minio: %s", message);
        return message;
    }
}
