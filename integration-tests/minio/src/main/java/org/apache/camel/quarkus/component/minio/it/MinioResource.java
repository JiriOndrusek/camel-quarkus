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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;

@Path("/minio")
@ApplicationScoped
public class MinioResource {

    public static final String SERVER_ACCESS_KEY = "testAccessKey";
    public static final String SERVER_SECRET_KEY = "testSecretKey";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/consumer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumer() {

        final String message = consumerTemplate.receiveBody(
                "minio://mycamel?moveAfterRead=true&destinationBucketName=camel-kafka-connector"
                        + "&accessKey=" + SERVER_ACCESS_KEY
                        + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")",
                5000, String.class);
        return message;
    }

    @Path("/operation")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String operation(String body,
            @QueryParam(MinioConstants.MINIO_OPERATION) String operation,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName,
            @QueryParam(MinioConstants.DESTINATION_OBJECT_NAME) String destinationObjectName,
            @QueryParam(MinioConstants.DESTINATION_BUCKET_NAME) String destinationBucketName,
            @QueryParam(MinioConstants.OFFSET) Integer offset,
            @QueryParam(MinioConstants.LENGTH) Integer length) {

        String endpoint = "minio:mycamel?accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")";

        MinioOperations op = (operation != "" && !"".equals(operation) ? MinioOperations.valueOf(operation) : null);

        Map<String, Object> headers = new HashMap<>();
        if (op != null) {
            headers.put(MinioConstants.MINIO_OPERATION, op);
        }
        if (objectName != null) {
            headers.put(MinioConstants.OBJECT_NAME, objectName);
        }
        if (destinationObjectName != null) {
            headers.put(MinioConstants.DESTINATION_OBJECT_NAME, destinationObjectName);
        }
        if (destinationBucketName != null) {
            headers.put(MinioConstants.DESTINATION_BUCKET_NAME, destinationBucketName);
        }
        if (offset != null) {
            headers.put(MinioConstants.OFFSET, offset);
        }
        if (length != null) {
            headers.put(MinioConstants.LENGTH, length);
        }

        if (op == MinioOperations.getObject) {
            return producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class);
        }

        final Iterable objectList = producerTemplate.requestBodyAndHeaders(endpoint, body, headers, Iterable.class);
        StringBuilder sb = new StringBuilder();
        StringBuilder errorSB = new StringBuilder();
        objectList.forEach(r -> {
            try {
                if (r instanceof Result) {
                    Object o = ((Result) r).get();
                    if (o instanceof Item) {
                        sb.append("item: ").append(((Item) o).objectName());
                    } else {
                        sb.append(o);
                    }
                } else if (r instanceof Bucket) {
                    sb.append("bucket: ").append(((Bucket) r).name());
                } else if (r instanceof GetObjectResponse) {
                    if(length != null && offset != null) {
                        byte[] bytes = new byte[length];
                        ((GetObjectResponse)r).read(bytes, 0, length-offset);
                        sb.append(new String(bytes, StandardCharsets.UTF_8));
                    } else {
                        errorSB.append("Offset and length is required!");
                    }

                } else {
                    sb.append(r);
                }
                sb.append(", ");
            } catch (Exception e) {
                errorSB.append(e.toString());
            }
        });
        return errorSB.length() > 0 ? errorSB.toString() : sb.toString();
    }

    @Path("/getUsingPojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String getUsingPojo(String bucket,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName) {

        String endpoint = "minio:mycamel?accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")"
                + "&pojoRequest=true"
                + "&minioClient=#minioClient";

        GetObjectArgs.Builder body = GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName);

        Map<String, Object> headers = Collections.singletonMap(MinioConstants.MINIO_OPERATION, MinioOperations.getObject);

        return producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class);

    }
}
