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
package org.apache.camel.quarkus.component.shiro.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;
import org.apache.camel.component.shiro.security.ShiroSecurityTokenInjector;
import org.jboss.logging.Logger;

@Path("/shiro")
@ApplicationScoped
public class ShiroResource {

    private static final Logger LOG = Logger.getLogger(ShiroResource.class);

    public static byte[] passPhrase = {
            (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B,
            (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F,
            (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13,
            (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17 };

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    //    @Inject
    //    ConsumerTemplate consumerTemplate;
    //
    //    @Path("/get")
    //    @GET
    //    @Produces(MediaType.TEXT_PLAIN)
    //    public String get() throws Exception {
    //        final String message = consumerTemplate.receiveBodyNoWait("shiro:--fix-me--", String.class);
    //        LOG.infof("Received from shiro: %s", message);
    //        return message;
    //    }

    @Path("/base64")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(ShiroSecurityToken shiroSecurityToken, @QueryParam("expectSuccess") boolean expextSuccess)
            throws Exception {
        TestShiroSecurityTokenInjector shiroSecurityTokenInjector = new TestShiroSecurityTokenInjector(shiroSecurityToken,
                passPhrase);

        MockEndpoint mockEndpointSuccess = context.getEndpoint("mock:success", MockEndpoint.class);
        MockEndpoint mockEndpointFailure = context.getEndpoint("mock:authenticationException", MockEndpoint.class);

        mockEndpointSuccess.reset();
        mockEndpointFailure.reset();

        mockEndpointSuccess.expectedMessageCount(expextSuccess ? 1 : 0);
        mockEndpointFailure.expectedMessageCount(expextSuccess ? 0 : 1);

        producerTemplate.send("direct:secureEndpoint", shiroSecurityTokenInjector);

        mockEndpointSuccess.assertIsSatisfied();
        mockEndpointFailure.assertIsSatisfied();
    }

    private static class TestShiroSecurityTokenInjector extends ShiroSecurityTokenInjector {

        TestShiroSecurityTokenInjector(ShiroSecurityToken shiroSecurityToken, byte[] bytes) {
            super(shiroSecurityToken, bytes);
            setBase64(true);
        }
    }
}
