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

import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;

public class DebeziumOracleTestResource extends AbstractDebeziumTestResource<GenericContainer<?>> {

    public static final String DB_USERNAME = "oracleUser";
    public static final String DB_PASSWORD = "changeit";
    //    private static final int DB_PORT = 5432;

    public DebeziumOracleTestResource() {
        super(Type.oracle);
    }

    @Override
    protected GenericContainer<?> createContainer() {
        return null;
    }

    @Override
    protected String getHost() {
        return "localhost";
    }

    @Override
    protected int getExtPort() {
        return 12345;
    }

    @Override
    protected String getJdbcUrl() {
//        return ConfigProvider.getConfig().getValue("quarkus.datasource.oracle.jdbc.url", String.class);
        return "jdbc:oracle:thin:@localhost:12345/oracle";
    }


    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return 12345;
    }
}
