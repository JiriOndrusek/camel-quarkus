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

package org.apache.camel.quarkus.component.sql.it;

import java.io.File;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * TODO
 */
public class DerbyTestResource<T extends GenericContainer> implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DerbyTestResource.class);

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            File[] jars = new File(Thread.currentThread().getContextClassLoader().getResource("/").toURI())
                    .listFiles((d, n) -> n.startsWith("camel-quarkus-integration-test-sql-derby-stored-procedure"));
            if (jars.length != 1) {
                String msg = "There has to be one jar in target/test-classes with the name \"camel-quarkus-integration-test-sql-derby-stored-procedure-*.jar\", which contains stored procedure for the derby db.";
                LOGGER.error(msg);
                throw new IllegalStateException(msg);
            }

            container = new GenericContainer("az82/docker-derby")
                    //                    .withFixedExposedPort(1527, 1527)
                    .withExposedPorts(1527)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(jars[0].getName()),
                            "/dbs/storedProcedure.jar")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            Map<String, String> map = CollectionHelper.mapOf("camel.sql.derby.port", container.getMappedPort(1527) + "");

            return map;

        } catch (Exception e) {
            LOGGER.error("Container does not start", e);
            throw new RuntimeException(e);
        }
    }

    protected void startContainer() throws Exception {
        container.start();
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
