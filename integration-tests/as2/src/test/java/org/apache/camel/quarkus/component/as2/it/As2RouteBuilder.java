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

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.support.PropertyBindingSupport;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class As2RouteBuilder extends RouteBuilder {

    private static final String TEST_OPTIONS_PROPERTIES = "/test-options.properties";

        @Override
        public void addRoutesToCamelContext(CamelContext context) throws Exception {
            //prepare component in camel context
            // create AS2 component configuration
            Map<String, Object> options = new HashMap<>();
            //host name of host messages sent to
            options.put("targetHostname","localhost");
            //port number of host messages sent to
            options.put("targetPortNumber", System.getProperty(As2ClientTest.PORT_PARAMETER));
            //fully qualified domain name used in Message-Id message header.
            options.put("clientFqdn", "example.com");
            //port number listened on
            options.put("serverPortNumber", System.getProperty(As2ClientTest.PORT_PARAMETER));

            final AS2Configuration configuration = new AS2Configuration();
            PropertyBindingSupport.bindProperties(context, configuration, options);

            // add AS2Component to Camel context
            final AS2Component component = new AS2Component(context);
            component.setConfiguration(configuration);
            context.addComponent("as2", component);


            super.addRoutesToCamelContext(context);
        }

    @Override
    public void configure() throws Exception {

        Processor proc = new Processor() {
            public void process(org.apache.camel.Exchange exchange) {
                HttpMessage message = exchange.getIn(HttpMessage.class);
                @SuppressWarnings("unused")
                String body = message.getBody(String.class);
            }
        };

        from("as2://server/listen?requestUriPattern=/")
                .to("mock:as2RcvMsgs");

        //        from("netty:tcp://0.0.0.0:8888/handle-receipts").process(proc)
        //                from("netty:tcp://0.0.0.0:8888/").process(proc)
        //                        .to("mock:result");
        //        from("direct://SEND").to("as2://client/send?inBody=ediMessage");
        //        from("jetty:http://localhost:8888/").process(proc);
    }
}
