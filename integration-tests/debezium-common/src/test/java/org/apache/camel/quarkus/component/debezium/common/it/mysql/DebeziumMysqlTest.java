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
package org.apache.camel.quarkus.component.debezium.common.it.mysql;

import java.sql.Connection;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.DebeziumMysqlResource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(DebeziumMysqlTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMysqlTest extends AbstractDebeziumTest {
    /**
     * Connection is handled by DebeziumMysqlTestResource (which also takes care of closing)
     */
    private static Connection connection;

    @Override
    protected String getJdbcUrl() {
        return "jdbc:mysql://" + System.getProperty(DebeziumMysqlResource.PROPERTY_HOSTNAME) + ":"
                + System.getProperty(DebeziumMysqlResource.PROPERTY_PORT) + "/" + DebeziumMysqlTestResource.DB_NAME + "?user="
                + DebeziumMysqlResource.DB_USERNAME + "&password=" + DebeziumMysqlResource.DB_PASSWORD;
    }

    @Test
    @Order(0)
    public void receiveEmptyMessages() {
        //receive all empty messages before other tests
        receiveResponse("receiveEmptyMessages")
                .then()
                .statusCode(204);
    }
}
