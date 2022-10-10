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

package org.apache.camel.quarkus.component.cxf.soap.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class CxfClientTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int WILDFLY_PORT = 8080;
    private GenericContainer<?> helloWorldContainer;
    private GenericContainer<?> skewedHelloWorldContainer;

    @Override
    public Map<String, String> start() {

        try {
            helloWorldContainer = new GenericContainer<>("cxf-soap/hello8:latest")
                    .withExposedPorts(WILDFLY_PORT)
                    .waitingFor(Wait.forHttp("/helloworld-ws/HelloService?wsdl"));

//            helloWorldContainer.start();

            //            skewedHelloWorldContainer = new GenericContainer<>("cxf-soap/hello:latest")
            //                    .withEnv("ADD_TO_RESULT", "100")
            //                    .withExposedPorts(WILDFLY_PORT)
            //                    .waitingFor(Wait.forHttp("/helloworld-ws/HelloWorldService?wsdl"));
            //
            //            skewedHelloWorldContainer.start();

            return Map.of(
                    "camel-quarkus.it.helloWorld.baseUri",
                    "http://" + helloWorldContainer.getHost() + ":8080"/* + helloWorldContainer
                            .getMappedPort(WILDFLY_PORT)/*,
                                                        "camel-quarkus.it.skewed-helloWorld.baseUri",
                                                        "http://" + skewedHelloWorldContainer.getHost() + ":"
                                                        + skewedHelloWorldContainer.getMappedPort(WILDFLY_PORT)*/);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (helloWorldContainer != null) {
                helloWorldContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (skewedHelloWorldContainer != null) {
                skewedHelloWorldContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
