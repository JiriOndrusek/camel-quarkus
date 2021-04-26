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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.StorageOptions;
import io.quarkiverse.googlecloudservices.storage.runtime.StorageProducer;
import io.quarkus.arc.Unremovable;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.storage.GoogleCloudStorageComponent;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.jboss.logging.Logger;

@Path("/google-storage")
@ApplicationScoped
public class GoogleStorageResource {

    public static final String DIRECT_POLLING = "direct:polling";

    public static final String PARAM_PORT = "org.apache.camel.quarkus.component.googlr.storage.it.GoogleStorageClientProducer_port";

    public static final String QUERY_OBJECT_NAME = "objectName";
    public static final String QUERY_BUCKET = "bucketName";
    public static final String QUERY_OPERATION = "operation";
    public static final String QUERY_DESTINATION_BUCKET = "destinationBucket";
    public static final String QUERY_DIRECT = "fromDirect";
    public static final String QUERY_POLLING_ACTION = "pollingAction";

    private static final Logger LOG = Logger.getLogger(GoogleStorageResource.class);
    private static final String COMPONENT_GOOGLE_STORAGE = "google-storage";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    StorageProducer sp;

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named(COMPONENT_GOOGLE_STORAGE)
    GoogleCloudStorageComponent produceComponent() throws IOException {
        GoogleCloudStorageComponent gsc = new GoogleCloudStorageComponent();
        if (!GoogleStorageHelper.isRealAccount()) {
            String port = System.getProperty(GoogleStorageResource.PARAM_PORT);
            gsc.getConfiguration().setStorageClient(StorageOptions.newBuilder()
                    .setHost("http://localhost:" + port)
                    .setProjectId("dummy-project-for-testing")
                    .build()
                    .getService());
        } else {
            gsc.getConfiguration().setStorageClient(sp.storage());
        }
        return gsc;
    }

    @Path("/operation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String operation(Map<String, Object> parameters,
            @QueryParam(QUERY_OPERATION) String operation,
            @QueryParam(QUERY_BUCKET) String bucketName) throws Exception {
        GoogleCloudStorageOperations op = GoogleCloudStorageOperations.valueOf(operation);
        String url = getBaseUrl(bucketName, "operation=" + op.toString());
        final Object response = producerTemplate.requestBodyAndHeaders(url, null, parameters, Object.class);
        if (response instanceof Blob) {
            return new String(((Blob) response).getContent());
        }
        if (response instanceof CopyWriter) {
            return new String(((CopyWriter) response).getResult().getContent());
        }
        if (response instanceof List) {
            List l = (List) response;
            return (String) l.stream().map(o -> {
                if (o instanceof Bucket) {
                    return ((Bucket) o).getName();
                }
                if (o instanceof Blob) {
                    return ((Blob) o).getName();
                }
                return "null";
            }).collect(Collectors.joining(","));
        }
        return String.valueOf(response);
    }

    @Path("/putObject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putObject(String body,
            @QueryParam(QUERY_BUCKET) String bucketName,
            @QueryParam(QUERY_OBJECT_NAME) String objectName) throws Exception {
        String url = getBaseUrl(bucketName, "autoCreateBucket=true");
        final Blob response = producerTemplate.requestBodyAndHeader(url,
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
            String url = String.format(getBaseUrl(bucketName, "autoCreateBucket=true")
                    + "&%s=true"
                    + "&destinationBucket=%s"
                    + "&deleteAfterRead=true"
                    + "&includeBody=true", pollingAction, destinationBucket);
            byte[] body = consumerTemplate.receiveBody(url, GoogleStorageHelper.isRealAccount() ? 10000 : 5000, byte[].class);
            producerTemplate.sendBody(DIRECT_POLLING, "Polling Hello " + new String(body));
        });
    }

    @Path("/getFromDirect")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getFromDirect(@QueryParam(QUERY_DIRECT) String directName) {
        return consumerTemplate.receiveBody(directName, GoogleStorageHelper.isRealAccount() ? 5000 : 1000, String.class);
    }

    private String getBaseUrl(String bucketName, String parameters) {
        //        if (GoogleStorageHelper.isRealAccount()) {
        //            return "google-storage://" + bucketName + "?serviceAccountKey=file:" + GoogleStorageHelper.getAccountLocation() + "&" + parameters;
        //        }
        return "google-storage://" + bucketName + "?" + parameters;
    }
}
