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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.support.PropertyBindingSupport;

@ApplicationScoped
public class As2RouteBuilder extends RouteBuilder {

    @Override
    public void addRoutesToCamelContext(CamelContext context) throws Exception {

        //prepare client component in camel context

        // create AS2 component configuration
        Map<String, Object> clientOptions = new HashMap<>();
        //host name of host messages sent to
        clientOptions.put("targetHostname", "localhost");
        //port number of host messages sent to
        clientOptions.put("targetPortNumber", System.getProperty(As2Resource.CLIENT_PORT_PARAMETER));
        //fully qualified domain name used in Message-Id message header.
        clientOptions.put("clientFqdn", "example.com");
        //port number listened on
        clientOptions.put("serverPortNumber", System.getProperty(As2Resource.CLIENT_PORT_PARAMETER));

        final AS2Configuration clientConfiguration = new AS2Configuration();
        PropertyBindingSupport.bindProperties(context, clientConfiguration, clientOptions);

        // add client AS2Component into Camel context
        final AS2Component clientComponent = new AS2Component(context);
        clientComponent.setConfiguration(clientConfiguration);
        context.addComponent("as2-client", clientComponent);

        //prepare server component in camel context

        // create AS2 component configuration
        Map<String, Object> serverOptions = new HashMap<>();
        //host name of host messages sent to
        serverOptions.put("targetHostname", "localhost");
        //port number of host messages sent to
        serverOptions.put("targetPortNumber", System.getProperty(As2Resource.SERVER_PORT_PARAMETER));
        //fully qualified domain name used in Message-Id message header.
        serverOptions.put("clientFqdn", "example.com");
        //port number listened on
        serverOptions.put("serverPortNumber", System.getProperty(As2Resource.SERVER_PORT_PARAMETER));

        final AS2Configuration serverConfiguration = new AS2Configuration();
        PropertyBindingSupport.bindProperties(context, serverConfiguration, serverOptions);

        // add server AS2Component into Camel context
        final AS2Component serverComponent = new AS2Component(context);
        serverComponent.setConfiguration(serverConfiguration);
        context.addComponent("as2-server", serverComponent);

        super.addRoutesToCamelContext(context);
    }

    @Override
    public void configure() throws Exception {
        from("as2-server://server/listen?requestUriPattern=/")
                .to("mock:as2RcvMsgs");
    }
}
