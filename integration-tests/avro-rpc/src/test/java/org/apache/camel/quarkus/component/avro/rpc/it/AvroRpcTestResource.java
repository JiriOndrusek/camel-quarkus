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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.jetty.HttpServer;
import org.apache.avro.ipc.netty.NettyServer;
import org.apache.avro.ipc.netty.NettyTransceiver;
import org.apache.avro.ipc.reflect.ReflectRequestor;
import org.apache.avro.ipc.reflect.ReflectResponder;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflectionImpl;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;

public class AvroRpcTestResource implements QuarkusTestResourceLifecycleManager {

    //server implementations
    TestReflection testReflection = new TestReflectionImpl();

    //avro servers listening on localhost
    Server httpServerReflection, nettyServerReflection;

    SpecificRequestor nettyReflectRequestor, httpReflectRequestor;
    Transceiver httpReflectTransceiver, nettyReflectTransceiver;

    @Override
    public Map<String, String> start() {
        try {
            final int httpPort = AvailablePortFinder.getNextAvailable();
            httpServerReflection = new HttpServer(
                    new ReflectResponder(TestReflection.class, testReflection),
                    httpPort);
            httpServerReflection.start();

            final int nettyPort = AvailablePortFinder.getNextAvailable();
            httpServerReflection = new NettyServer(
                    new ReflectResponder(TestReflection.class, testReflection),
                    new InetSocketAddress(nettyPort));
            httpServerReflection.start();

            final int httpTranscieverPort = AvailablePortFinder.getNextAvailable();
            httpReflectTransceiver = new HttpTransceiver(new URL("http://localhost:" + httpTranscieverPort));
            httpReflectRequestor = new ReflectRequestor(TestReflection.class, httpReflectTransceiver);

            final int nettyTranscieverPort = AvailablePortFinder.getNextAvailable();
            nettyReflectTransceiver = new NettyTransceiver(new InetSocketAddress("localhost", nettyTranscieverPort));
            nettyReflectRequestor = new ReflectRequestor(TestReflection.class, nettyReflectTransceiver);

            return CollectionHelper.mapOf(AvroRpcResource.REFLECTIVE_HTTP_SERVER_PORT_PARAM, String.valueOf(httpPort),
                    AvroRpcResource.REFLECTIVE_NETTY_SERVER_PORT_PARAM, String.valueOf(nettyPort),
                    AvroRpcResource.REFLECTIVE_HTTP_CONSUMER_PORT_PARAM, String.valueOf(httpTranscieverPort),
                    AvroRpcResource.REFLECTIVE_NETTY_CONSUMER_PORT_PARAM, String.valueOf(nettyTranscieverPort));
        } catch (Exception e) {
            throw new RuntimeException("Could not start avro-rpc server", e);
        }
    }

    @Override
    public void stop() {
        if (httpServerReflection != null) {
            httpServerReflection.close();
        }
        if (nettyServerReflection != null) {
            nettyServerReflection.close();
        }
        if (nettyReflectTransceiver != null) {
            try {
                nettyReflectTransceiver.close();
            } catch (IOException e) {
                //ignore
            }
        }
        if (httpReflectTransceiver != null) {
            try {
                httpReflectTransceiver.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void inject(Object testInstance) {
        AvroRpcTestSupport testSupport = (AvroRpcTestSupport) testInstance;
        ((AvroRpcTestSupport) testInstance).setTestReflection(testReflection);
        ((AvroRpcTestSupport) testInstance)
                .setReflectRequestor(testSupport.isHttp() ? httpReflectRequestor : nettyReflectRequestor);
    }
}
