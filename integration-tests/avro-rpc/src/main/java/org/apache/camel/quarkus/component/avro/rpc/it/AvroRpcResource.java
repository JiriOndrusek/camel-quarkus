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
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.Key;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.Value;
import org.apache.camel.quarkus.component.avro.rpc.it.impl.TestReflectionImpl;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;

@Path("/avro-rpc")
@ApplicationScoped
public class AvroRpcResource {

    public static final String REFLECTIVE_HTTP_SERVER_PORT_PARAM = "camel.avro-rpc.test.reflective.httpServerReflection.port";
    public static final String REFLECTIVE_NETTY_SERVER_PORT_PARAM = "camel.avro-rpc.test.reflective.nettyServerReflection.port";
    public static final String GENERATED_HTTP_SERVER_PORT_PARAM = "camel.avro-rpc.test.generated.httpServerReflection.port";
    public static final String GENERATED_NETTY_SERVER_PORT_PARAM = "camel.avro-rpc.test.generated.nettyServerReflection.port";
    public static final String REFLECTIVE_HTTP_CONSUMER_PORT_PARAM = "camel.avro-rpc.test.httpConsumerReflection.port";
    public static final String REFLECTIVE_NETTY_CONSUMER_PORT_PARAM = "camel.avro-rpc.test.nettyConsumerReflection.port";

    private TestReflection httpTtestReflection = new TestReflectionImpl(),
            nettyTestReflection = new TestReflectionImpl();

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/reflectionProducer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void reflectionProducer(@QueryParam("protocol") ProtocolType protocol, String name) throws Exception {
        Object[] request = { name };
        producerTemplate.requestBody(String.format(
                "avro:%s:localhost:{{%s}}/setName?protocolClassName=%s&singleParameter=true",
                protocol,
                protocol == ProtocolType.http ? REFLECTIVE_HTTP_SERVER_PORT_PARAM : REFLECTIVE_NETTY_SERVER_PORT_PARAM,
                TestReflection.class.getCanonicalName()), request);
    }

    @Path("/generatedProducerPut")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void generatedProducerPut(@QueryParam("protocol") ProtocolType protocol, @QueryParam("key") String key, String value)
            throws Exception {
        Key k = Key.newBuilder().setKey(key).build();
        Value v = Value.newBuilder().setValue(value).build();

        Object[] request = { k, v };
        producerTemplate.requestBody(String.format(
                "avro:%s:localhost:{{%s}}/put?protocolClassName=%s",
                protocol,
                protocol == ProtocolType.http ? GENERATED_HTTP_SERVER_PORT_PARAM : GENERATED_NETTY_SERVER_PORT_PARAM,
                KeyValueProtocol.class.getCanonicalName()), request);
    }

    @Path("/generatedProducerGet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String generatedProducerGet(@QueryParam("protocol") ProtocolType protocol, String key) throws Exception {
        Key k = Key.newBuilder().setKey(key).build();

        Object[] request = { k };
        return producerTemplate.requestBody(String.format(
                "avro:%s:localhost:{{%s}}/get?protocolClassName=%s&singleParameter=true",
                protocol,
                protocol == ProtocolType.http ? GENERATED_HTTP_SERVER_PORT_PARAM : GENERATED_NETTY_SERVER_PORT_PARAM,
                KeyValueProtocol.class.getCanonicalName()), request, String.class);
    }

    @Path("/reflectionConsumer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String reflectionConsumer(ProtocolType protocol) throws Exception {
        return getTestReflection(protocol).getTestPojo().getPojoName();
    }

    public TestReflection getTestReflection(ProtocolType protocol) {
        return protocol == ProtocolType.http ? httpTtestReflection : nettyTestReflection;
    }
}
