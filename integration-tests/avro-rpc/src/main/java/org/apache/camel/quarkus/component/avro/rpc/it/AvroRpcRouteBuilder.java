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

package org.apache.camel.quarkus.component.avro.rpc.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.ReflectionProcessor;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.PutProcessor;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.KeyValueProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AvroRpcRouteBuilder extends RouteBuilder {
    public static final String DIRECT_START = "direct:start";

    Integer httpPort = 8081;

    @ConfigProperty(name = AvroRpcResource.REFLECTIVE_NETTY_TRANSCEIVER_PORT_PARAM)
    Integer nettyPort;

    Integer specificHttpPort = 8081;

    @ConfigProperty(name = AvroRpcResource.SPECIFIC_NETTY_TRANSCEIVER_PORT_PARAM)
    Integer specificNettyPort;

    @Inject
    AvroRpcResource avroRpcResource;

    @Override
    public void configure() throws Exception {

        from(String.format("avro:http:localhost:%d/setTestPojo?protocolClassName=%s&singleParameter=true", httpPort,
                TestReflection.class.getCanonicalName()))
                        .process(new ReflectionProcessor(avroRpcResource.getTestReflection(ProtocolType.http)));

        from(String.format("avro:netty:localhost:%d/setTestPojo?protocolClassName=%s&singleParameter=true", nettyPort,
                TestReflection.class.getCanonicalName()))
                        .process(new ReflectionProcessor(avroRpcResource.getTestReflection(ProtocolType.netty)));

        from(String.format("avro:http:localhost:%d/put?protocolClassName=%s", specificHttpPort,
                KeyValueProtocol.class.getCanonicalName()))
                        .process(new PutProcessor(avroRpcResource.getKeyValue(ProtocolType.http)));

        from(String.format("avro:netty:localhost:%d/put?protocolClassName=%s", specificNettyPort,
                KeyValueProtocol.class.getCanonicalName()))
                        .process(new PutProcessor(avroRpcResource.getKeyValue(ProtocolType.netty)));

    }
}
