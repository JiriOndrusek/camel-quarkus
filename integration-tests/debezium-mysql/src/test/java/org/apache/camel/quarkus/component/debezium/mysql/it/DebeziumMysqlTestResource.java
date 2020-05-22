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

package org.apache.camel.quarkus.component.debezium.mysql.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public class DebeziumMysqlTestResource implements ContainerResourceLifecycleManager {
    public static final String DB_NAME = "test";

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumMysqlTestResource.class);

    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_IMAGE = "mysql:5.7";

    private MySQLContainer<?> mySQLContainer;
    private Path storeFile, historyFile;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            storeFile = Files.createTempFile("debezium-mysql-store-", "");
            historyFile = Files.createTempFile("debezium-mysql-history-file-", "");

            mySQLContainer = new MySQLContainer<>(MYSQL_IMAGE)
                    .withUsername(DebeziumMysqlResource.DB_USERNAME)
                    .withPassword(DebeziumMysqlResource.DB_PASSWORD)
                    .withDatabaseName(DB_NAME)
                    .withInitScript("init.sql");

            mySQLContainer.start();

            return CollectionHelper.mapOf(
                    DebeziumMysqlResource.PROPERTY_HOSTNAME, mySQLContainer.getContainerIpAddress(),
                    DebeziumMysqlResource.PROPERTY_PORT, mySQLContainer.getMappedPort(MYSQL_PORT) + "",
                    DebeziumMysqlResource.PROPERTY_DB_HISTORY_FILE, historyFile.toString(),
                    DebeziumMysqlResource.PROPERTY_OFFSET_STORE_FILEPORT, storeFile.toString());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (mySQLContainer != null) {
                mySQLContainer.stop();
            }
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
            if (historyFile != null) {
                Files.deleteIfExists(historyFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
