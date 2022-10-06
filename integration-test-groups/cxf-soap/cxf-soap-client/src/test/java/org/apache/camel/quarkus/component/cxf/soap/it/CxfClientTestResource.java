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
    private GenericContainer<?> calculatorContainer;
    private GenericContainer<?> skewedCalculatorContainer;

    @Override
    public Map<String, String> start() {

        try {
            calculatorContainer = new GenericContainer<>("cxf-soap/hello:latest")
                    .withExposedPorts(WILDFLY_PORT)
                    .waitingFor(Wait.forHttp("/helloworld-ws/HelloWorldService?wsdl"));

            calculatorContainer.start();

//            skewedCalculatorContainer = new GenericContainer<>("6488a3e3dc56")
//                    .withEnv("ADD_TO_RESULT", "100")
//                    .withExposedPorts(WILDFLY_PORT)
//                    .waitingFor(Wait.forHttp("/helloworld-ws/HelloWorldService?wsdl"));
//
//            skewedCalculatorContainer.start();

            return Map.of(
                    "camel-quarkus.it.helloWorld.baseUri",
                    "http://" + calculatorContainer.getHost() + ":" + calculatorContainer.getMappedPort(WILDFLY_PORT)/*,
                    "cxf.it.skewed-calculator.baseUri",
                    "http://" + skewedCalculatorContainer.getHost() + ":"
                            + skewedCalculatorContainer.getMappedPort(WILDFLY_PORT)*/);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (calculatorContainer != null) {
                calculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (skewedCalculatorContainer != null) {
                skewedCalculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
