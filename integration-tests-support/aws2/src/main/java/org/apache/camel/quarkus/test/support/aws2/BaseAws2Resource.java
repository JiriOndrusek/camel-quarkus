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
package org.apache.camel.quarkus.test.support.aws2;

import javax.enterprise.event.Observes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.ShutdownEvent;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;

public class BaseAws2Resource {

    private static final Logger LOG = Logger.getLogger(BaseAws2Resource.class);

    protected boolean useDefaultCredentials;

    private final LocalStackContainer.Service service;

    private boolean clearAwsSystemCredentials;

    public BaseAws2Resource(LocalStackContainer.Service service) {
        this.service = service;
    }

    @Path("/setUseDefaultCredentialsProvider")
    @POST
    public Response setUseDefaultCredentials(boolean useDefaultCredentialsProvider) throws Exception {
        LOG.info(
                "Setting setUseDefaultCredentials to " + useDefaultCredentialsProvider);
        this.useDefaultCredentials = useDefaultCredentialsProvider;
        return Response.ok().build();
    }

    @Path("/initializeDefaultCredentials")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response initializeDefaultCredentials(boolean initialize) throws Exception {
        String s = Aws2Helper.camelServiceAcronym(service);

        if (initialize) {
            //set flag to clear at the end
            clearAwsSystemCredentials = true;
            LOG.debug(
                    "Setting both System.properties `aws.secretAccessKey` and `aws.accessKeyId` to cover defaultCredentialsProviderTest.");
            //defaultCredentials provider gets the credentials from fixed location. One of them is system.properties,
            //therefore to succeed the test, system.properties has to be initialized with the values from the configuration
            Aws2Helper.setAwsSysteCredentials(
                    ConfigProvider.getConfig().getValue("camel.component.aws2-" + s + ".access-key", String.class),
                    ConfigProvider.getConfig().getValue("camel.component.aws2-" + s + ".secret-key", String.class));

        } else {
            LOG.debug("Clearing both System.properties `aws.secretAccessKey` and `aws.accessKeyId`.");
            Aws2Helper.clearAwsSysteCredentials();
            clearAwsSystemCredentials = false;
        }

        return Response.ok().build();
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (clearAwsSystemCredentials) {
            Aws2Helper.clearAwsSysteCredentials();
        }
    }

    public boolean isUseDefaultCredentials() {
        return useDefaultCredentials;
    }
}
