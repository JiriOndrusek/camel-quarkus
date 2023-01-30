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
package org.apache.camel.quarkus.component.aws2.kinesis.it;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/aws2-kinesis")
@ApplicationScoped
public class Aws2KinesisResource {

    private static final Logger log = Logger.getLogger(Aws2KinesisResource.class);

    @ConfigProperty(name = "aws-kinesis.stream-name")
    String streamName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @Named("aws2KinesisMessages")
    Queue<String> aws2KinesisMessages;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(String message,
            @QueryParam("useDefaultCredentialsProvider") boolean useDefaultCredentialsProvider,
            @QueryParam("setSystemCredentials") boolean setSystemCredentials) throws Exception {

        try {
            if (setSystemCredentials) {
                //defaultCredentials provider gets the credentials from fixed location. One of them is system.properties,
                //therefore to succeed the test, system.properties has to be initialized with the values from the configuration
                System.setProperty("aws.accessKeyId",
                        ConfigProvider.getConfig().getValue("camel.component.aws2-kinesis.access-key", String.class));
                System.setProperty("aws.secretAccessKey",
                        ConfigProvider.getConfig().getValue("camel.component.aws2-kinesis.secret-key", String.class));
            }
            final String response = producerTemplate.requestBodyAndHeader(
                    componentUri(useDefaultCredentialsProvider),
                    message,
                    Kinesis2Constants.PARTITION_KEY,
                    "foo-partition-key",
                    String.class);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        } finally {
            if (setSystemCredentials) {
                //system properties has to be cleared after the test
                System.clearProperty("aws.accessKeyId");
                System.clearProperty("aws.secretAccessKey");
            }
        }
    }

    @Path("/receive/")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive(@QueryParam("useDefaultCredentialsProvider") boolean useDefaultCredentialsProvider)
            throws IOException {
        return aws2KinesisMessages.poll();
    }

    private String componentUri(boolean useDefaultCredentialsProvider) {
        return "aws2-kinesis://" + streamName + "?useDefaultCredentialsProvider=" + useDefaultCredentialsProvider;
    }
}
