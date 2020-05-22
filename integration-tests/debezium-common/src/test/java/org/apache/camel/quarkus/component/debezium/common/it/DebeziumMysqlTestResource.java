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

import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

public class DebeziumMysqlTestResource extends AbstractDebeziumTestResource {
    public static final String DB_NAME = "test";

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumMysqlTestResource.class);

    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_IMAGE = "mysql:5.7";

    private Path historyFile;

    @Override
    GenericContainer createContainer() {
        return new MySQLContainer<>(MYSQL_IMAGE)
                .withUsername(DebeziumMysqlResource.DB_USERNAME)
                .withPassword(DebeziumMysqlResource.DB_PASSWORD)
                .withDatabaseName(DB_NAME)
                .withInitScript("initMysql.sql");
    }

    @Override
    Map<String, String> enhanceStart() {

        try {
            historyFile = Files.createTempFile(getClass().getName() + "-history-file-", "");

            return CollectionHelper.mapOf(
                    DebeziumMysqlResource.PROPERTY_HOSTNAME, container.getContainerIpAddress(),
                    DebeziumMysqlResource.PROPERTY_PORT, container.getMappedPort(MYSQL_PORT) + "",
                    DebeziumMysqlResource.PROPERTY_DB_HISTORY_FILE, historyFile.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    String getJdbcUrl() {
        return "jdbc:mysql://" + container.getContainerIpAddress() + ":" + container.getMappedPort(MYSQL_PORT) + "/"
                + DebeziumMysqlTestResource.DB_NAME + "?user=" + DebeziumMysqlResource.DB_USERNAME
                + "&password=" + DebeziumMysqlResource.DB_PASSWORD;
    }

    @Override
    public void stop() {
        super.stop();

        try {
            if (historyFile != null) {
                Files.deleteIfExists(historyFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
