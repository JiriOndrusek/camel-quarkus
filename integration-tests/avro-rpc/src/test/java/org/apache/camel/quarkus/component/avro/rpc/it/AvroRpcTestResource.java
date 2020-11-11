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

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.jetty.HttpServer;
import org.apache.avro.ipc.reflect.ReflectResponder;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflectionImpl;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;

public class AvroRpcTestResource implements QuarkusTestResourceLifecycleManager {

    //server implementations
    TestReflection testReflection = new TestReflectionImpl();

    //avro servers listening on localhost
    Server serverReflection;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();
            serverReflection = new HttpServer(
                    new ReflectResponder(TestReflection.class, testReflection),
                    port);
            serverReflection.start();

            return CollectionHelper.mapOf(AvroRpcResource.REFLECTIVE_SERVER_PORT_PARAM, String.valueOf(port));
        } catch (Exception e) {
            throw new RuntimeException("Could not start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        serverReflection.close();
    }

    @Override
    public void inject(Object testInstance) {
        ((AvroRpcTest) testInstance).setTestReflection(testReflection);
    }
}
