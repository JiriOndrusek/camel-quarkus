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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestPojo;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflectionImpl;

@Path("/avro-rpc")
@ApplicationScoped
public class AvroRpcResource {

    public static final String REFLECTIVE_HTTP_SERVER_PORT_PARAM = "camel.avro-rpc.test.httpServerReflection.port";
    public static final String REFLECTIVE_NETTY_SERVER_PORT_PARAM = "camel.avro-rpc.test.nettyServerReflection.port";
    public static final String REFLECTIVE_HTTP_CONSUMER_PORT_PARAM = "camel.avro-rpc.test.nettyConsumerReflection.port";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    AvroRpcRouteBuilder avroRpcRouteBuilder;


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

    @Path("/reflectionConsumer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TestPojo reflectionConsumer() throws Exception {
        Thread.sleep(5000);
        return avroRpcRouteBuilder.testReflection.getTestPojo();
    }


}
