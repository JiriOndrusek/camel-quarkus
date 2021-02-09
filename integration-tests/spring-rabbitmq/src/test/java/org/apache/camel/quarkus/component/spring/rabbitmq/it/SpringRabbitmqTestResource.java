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
package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class SpringRabbitmqTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRabbitmqTestResource.class);
    private static final String RABBITMQ_IMAGE = "rabbitmq:3.8.7-alpine";
    private static final int RABBITMQ_PORT = 5672;
    private static final String RABBITMQ_USERNAME = "guest";
    private static final String RABBITMQ_PASSWORD = "guest";

    private RabbitMQContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new RabbitMQContainer(RABBITMQ_IMAGE)
                    .withExposedPorts(RABBITMQ_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forListeningPort())
            .withQueue("myqueue")
            .withBinding("myqueue", "foo");

            container.start();

            return CollectionHelper.mapOf(
                    SpringRabbitmqResource.PARAMETER_PORT, container.getMappedPort(RABBITMQ_PORT).toString(),
                    SpringRabbitmqResource.PARAMETER_HOSTNAME, container.getHost(),
                    SpringRabbitmqResource.PARAMETER_USERNAME, RABBITMQ_USERNAME,
                    SpringRabbitmqResource.PARAMETER_PASSWORD, RABBITMQ_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }
}
