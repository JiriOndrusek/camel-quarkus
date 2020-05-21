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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(DebeziumMysqlTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMysqlTest {

    private static final Logger LOG = Logger.getLogger(DebeziumMysqlTest.class);

    private static String COMPANY_1 = "Best Company";
    private static String COMPANY_2 = "Even Better Company";
    private static String CITY_1 = "Prague";
    private static String CITY_2 = "Paris";

    private static int REPEAT_COUNT = 5;

    /** Connection is handled by DebeziumMysqlTestResource (which also takes care of closing) */
    Connection connection;

    @Test
    @Order(1)
    public void insert() throws SQLException {

        //receive all empty messages before insert
        receiveResponse("receiveEmptyMessages")
                .then()
                .statusCode(204);

        executeUpdate("INSERT INTO COMPANY (name, city) VALUES ('" + COMPANY_1 + "', '" + CITY_1 + "')");

        receiveResponse()
                .then()
                .statusCode(200)
                .body(containsString((COMPANY_1)));
    }

    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        executeUpdate("INSERT INTO COMPANY (name, city) VALUES ('" + COMPANY_2 + "', '" + CITY_2 + "')");

        //validate event in queue
        receiveResponse(200, containsString(COMPANY_2));

        executeUpdate("UPDATE COMPANY SET name = '" + COMPANY_2 + "_changed' WHERE city = '" + CITY_2 + "'");

        //validate event with delete is in queue
        receiveResponse(204, is(emptyOrNullString()));
        //validate event with create is in queue
        receiveResponse(200, containsString(COMPANY_2 + "_changed"));
    }

    @Test
    @Order(3)
    public void testDelete() throws SQLException {
        int res = executeUpdate("DELETE FROM COMPANY");

        for (int i = 0; i < res; i++) {
            //validate event with delete is in queue
            receiveResponse(204, is(emptyOrNullString()));
        }
    }

    private Response receiveResponse() {
        return receiveResponse("receive");
    }

    private Response receiveResponse(String method) {
        return RestAssured.get("/debezium-mysql/" + method);
    }

    private void receiveResponse(int statusCode, Matcher<String> stringMatcher) {
        receiveResponse().then()
                .statusCode(statusCode)
                .body(stringMatcher);
    }

    private int executeUpdate(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }
}
