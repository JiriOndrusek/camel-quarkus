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

package org.apache.camel.quarkus.component.debezium.common.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public abstract class AbstractDebeziumTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDebeziumTestResource.class);

    protected GenericContainer<?> container;
    private Path storeFile;

    protected abstract GenericContainer createContainer();

    protected abstract Map<String, String> enhanceStart();

    protected abstract String getJdbcUrl();

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            storeFile = Files.createTempFile(getClass().getName() + "-store-", "");

            container = createContainer();

            container.start();

            Map<String, String> map = CollectionHelper.mapOf(
                    DebeziumMysqlResource.PROPERTY_OFFSET_STORE_FILEPORT, storeFile.toString(),
                    AbstractDebeziumTest.PROPERTY_JDBC, getJdbcUrl());

            map.putAll(enhanceStart());

            return map;

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
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
