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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public class DebeziumMysqlTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumMysqlTestResource.class);

    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_IMAGE = "mysql:5.7";
    private static final String DB_NAME = "mysqlDB";

    private MySQLContainer<?> mySQLContainer;
    private Connection connection;
    private Path storeFile, historyFile;
    private String hostname;
    private int port;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            storeFile = Files.createTempFile("debezium-mysql-store-", "");
            historyFile = Files.createTempFile("debezium-mysql-history-file-", "");

            mySQLContainer = new MySQLContainer<>(MYSQL_IMAGE)
                    .withUsername("test")
                    .withPassword("test");
            //                    .withDatabaseName(DB_NAME)
            //                    .withInitScript("init.sql");
            ;

            mySQLContainer.start();

            hostname = mySQLContainer.getContainerIpAddress();
            port = mySQLContainer.getMappedPort(MYSQL_PORT);
            //            hostname  = "localhost";
            //            port = 3306;

            final String jdbcUrl = "jdbc:mysql://localhost:"
                    + port + "/test?user=root&password=test";
            //            final String jdbcUrl = "jdbc:mysql://" + hostname + ":"
            //                    + port + "/" + DB_NAME + "?user="
            //                    + DebeziumMysqlResource.DB_USERNAME + "&password=" + DebeziumMysqlResource.DB_PASSWORD;
            connection = DriverManager.getConnection(jdbcUrl);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE COMPANY(\n" +
                        "    NAME           VARCHAR(100) NOT NULL,\n" +
                        "    CITY           VARCHAR(100) NOT NULL,\n" +
                        "    PRIMARY KEY ( NAME )\n" +
                        ");");
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
            }
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

    @Override
    public void inject(Object testInstance) {
        ((DebeziumMysqlTest) testInstance).connection = this.connection;
        ((DebeziumMysqlTest) testInstance).hostname = this.hostname;
        ((DebeziumMysqlTest) testInstance).port = this.port;
        ((DebeziumMysqlTest) testInstance).offsetStorageFileName = this.storeFile.toString();
        ((DebeziumMysqlTest) testInstance).databaseHistoryFileFilename = this.historyFile.toString();
    }

}
