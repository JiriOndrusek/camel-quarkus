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
import java.util.Map;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.CopyWriter;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.jboss.logging.Logger;

@Path("/google-storage")
@ApplicationScoped
public class GoogleStorageResource {

    public static enum Operation {getObject, copyObject}

    public static final String DIRECT_POLLING = "direct:polling";

    public static final String PARAM_PORT = "org.apache.camel.quarkus.component.googlr.storage.it.GoogleStorageClientProducer_port";

    public static final String QUERY_OBJECT_NAME = "objectName";
    public static final String QUERY_BUCKET = "bucketName";
    public static final String QUERY_OPERATION = "operation";
    public static final String QUERY_DESTINATION_BUCKET = "destinationBucket";
    public static final String QUERY_DIRECT = "fromDirect";
    public static final String QUERY_POLLING_ACTION = "pollingAction";

    private static final Logger LOG = Logger.getLogger(GoogleStorageResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/operation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response operation(Map<String, Object> parameters,
                              @QueryParam(QUERY_OPERATION) String operation,
                              @QueryParam(QUERY_BUCKET) String bucketName) throws Exception {
        Operation op = Operation.valueOf(operation);
        StringBuilder sb = new StringBuilder("google-storage://").append(bucketName).append("?autoCreateBucket=true").append("&operation=").append(op.toString());
        final Object response = producerTemplate.requestBodyAndHeaders(
                sb.toString(), null, parameters, Object.class);

        if(response instanceof Blob) {
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(new String(((Blob)response).getContent()))
                    .build();
        }
        if(response instanceof CopyWriter) {
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(new String(((CopyWriter)response).getResult().getContent()))
                    .build();
        }
        return null;
    }

    @Path("/putObject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putBucket(String body,
            @QueryParam(QUERY_BUCKET) String bucketName,
            @QueryParam(QUERY_OBJECT_NAME) String objectName) throws Exception {
        final Blob response = producerTemplate.requestBodyAndHeader("google-storage://" + bucketName + "?autoCreateBucket=true",
                body,
                GoogleCloudStorageConstants.OBJECT_NAME, objectName, Blob.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }

    @Path("/startPolling")
    @POST
    public void startPolling(@QueryParam(QUERY_BUCKET) String bucketName,
                             @QueryParam(QUERY_POLLING_ACTION) String pollingAction,
                             @QueryParam(QUERY_DESTINATION_BUCKET) String destinationBucket) {
        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String url = String.format("google-storage://%s?"
                    + "%s=true"
                    + "&destinationBucket=%s"
                    + "&autoCreateBucket=true"
                    + "&deleteAfterRead=true"
                    + "&includeBody=true", bucketName, pollingAction, destinationBucket);
            byte[] body = consumerTemplate.receiveBody(url, byte[].class);
            producerTemplate.sendBody(DIRECT_POLLING, "Polling Hello " + new String(body));
        });
    }

    @Path("/getFromDirect")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getFromDirect(@QueryParam(QUERY_DIRECT) String directName) {
        return consumerTemplate.receiveBody(directName, 5000, String.class);
    }
}
