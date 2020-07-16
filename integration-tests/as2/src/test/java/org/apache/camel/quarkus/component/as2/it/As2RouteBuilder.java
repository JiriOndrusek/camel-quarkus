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

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpMessage;

@ApplicationScoped
public class As2RouteBuilder extends RouteBuilder {

    private static final String TEST_OPTIONS_PROPERTIES = "/test-options.properties";

    //    @Override
    //    public void addRoutesToCamelContext(CamelContext context) throws Exception {
    //
    //
    //        // read AS2 component configuration from TEST_OPTIONS_PROPERTIES
    //        final Properties properties = new Properties();
    //        try {
    //            properties.load(getClass().getResourceAsStream(TEST_OPTIONS_PROPERTIES));
    //        } catch (Exception e) {
    //            throw new IOException(String.format("%s could not be loaded: %s", TEST_OPTIONS_PROPERTIES, e.getMessage()),
    //                    e);
    //        }
    //
    //        Map<String, Object> options = new HashMap<>();
    //        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
    //            options.put(entry.getKey().toString(), entry.getValue());
    //        }
    //
    //        final AS2Configuration configuration = new AS2Configuration();
    //        PropertyBindingSupport.bindProperties(context, configuration, options);
    //
    //        // add AS2Component to Camel context
    //        final AS2Component component = new AS2Component(context);
    //        component.setConfiguration(configuration);
    //        context.addComponent("as2", component);
    //
    //        super.addRoutesToCamelContext(context);
    //    }

    @Override
    public void configure() throws Exception {

        Processor proc = new Processor() {
            public void process(org.apache.camel.Exchange exchange) {
                HttpMessage message = exchange.getIn(HttpMessage.class);
                @SuppressWarnings("unused")
                String body = message.getBody(String.class);
            }
        };

        //        from("netty:tcp://0.0.0.0:8888/handle-receipts").process(proc)
        //                from("netty:tcp://0.0.0.0:8888/").process(proc)
        //                        .to("mock:result");
        //        from("direct://SEND").to("as2://client/send?inBody=ediMessage");
        //        from("jetty:http://localhost:8888/").process(proc);
    }
}
