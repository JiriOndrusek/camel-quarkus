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

package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import java.util.Map;

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.DebeziumPostgresResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DebeziumPostgresTestResource extends AbstractDebeziumTestResource {

    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_IMAGE = "debezium/postgres:11";

    @Override
    protected GenericContainer createContainer() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withUsername(DebeziumPostgresResource.DB_USERNAME)
                .withPassword(DebeziumPostgresResource.DB_PASSWORD)
                .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                .withInitScript("initPostgres.sql");
    }

    @Override
    protected void enhanceStart(Map<String, String> map) {
        map.put(DebeziumPostgresResource.PROPERTY_HOSTNAME, container.getContainerIpAddress());
        map.put(DebeziumPostgresResource.PROPERTY_PORT, container.getMappedPort(POSTGRES_PORT) + "");
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + container.getContainerIpAddress() + ":"
                + container.getMappedPort(POSTGRES_PORT) + "/" + DebeziumPostgresResource.DB_NAME + "?user="
                + DebeziumPostgresResource.DB_USERNAME + "&password=" + DebeziumPostgresResource.DB_PASSWORD;
    }
}
