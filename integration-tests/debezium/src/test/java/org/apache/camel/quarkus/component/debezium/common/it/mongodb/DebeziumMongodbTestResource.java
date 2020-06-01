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

package org.apache.camel.quarkus.component.debezium.common.it.mongodb;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.DebeziumSqlserverResource;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.jboss.logging.Logger;
import org.testcontainers.containers.MSSQLServerContainer;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<MSSQLServerContainer> {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static int DB_PORT = 1433;

    private Path historyFile;

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    @Override
    protected MongoDBContainer createContainer() {
        return new MongoDBContainer()

    }

    @Override
    public Map<String, String> start() {
        //detect EULA for MsSql
        URL eulaUrl = Thread.currentThread().getContextClassLoader().getResource("container-license-acceptance.txt");
        if (eulaUrl == null) {
            LOG.warn(
                    "Ms SQL EAULA is not accepted. Container won't start. See https://camel.apache.org/camel-quarkus/latest/extensions/debezium-sqlserver.html#_usage for more details.");
            return Collections.emptyMap();
        }

        Map<String, String> properties = super.start();

        try {
            historyFile = Files.createTempFile(getClass().getSimpleName() + "-history-file-", "");

            properties.put(DebeziumSqlserverResource.PROPERTY_DB_HISTORY_FILE, historyFile.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
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

    @Override
    protected String getJdbcUrl() {
        final String jdbcUrl = container.getJdbcUrl() + ";databaseName=testDB;user=" + getUsername() + ";password="
                + getPassword();

        return jdbcUrl;
    }

    @Override
    protected String getUsername() {
        return container.getUsername();
    }

    @Override
    protected String getPassword() {
        return container.getPassword();
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }
}
