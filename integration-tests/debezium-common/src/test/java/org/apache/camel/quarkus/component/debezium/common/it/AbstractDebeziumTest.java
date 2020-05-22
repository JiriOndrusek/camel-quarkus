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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

public abstract class AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(AbstractDebeziumTest.class);

    static final String PROPERTY_JDBC = AbstractDebeziumTest.class.getName() + "_jdbc";

    protected static String COMPANY_1 = "Best Company";
    protected static String COMPANY_2 = "Even Better Company";
    protected static String CITY_1 = "Prague";
    protected static String CITY_2 = "Paris";
    private static int REPEAT_COUNT = 5;

    private static Connection connection;

    protected abstract String getResourcePath();

    @BeforeAll
    public static void setUp() throws SQLException {
        final String jdbcUrl = System.getProperty(PROPERTY_JDBC);

        connection = DriverManager.getConnection(jdbcUrl);
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    public void insert() throws SQLException {
        int i = 0;
        while (i++ < REPEAT_COUNT) {
            //it could happen that debeium is not initialoized in time of the insert, for that case is insert repeated
            //until debezium reacts (max number of repetition is 5, which takes max 10 seconds - because call of
            // /debezium-postgres/getEvent has 2 seconds timeout
            executeUpdate("INSERT INTO COMPANY (name, city) VALUES ('" + COMPANY_1 + "_" + i + "', '" + CITY_1 + "')");

            Response response = receiveResponse();

            //if status code is 204 (no response), try again
            if (response.getStatusCode() == 204) {
                LOG.debug("Response code 204. Debezium is not running yet, repeating (" + i + "/" + REPEAT_COUNT + ")");
                continue;
            }

            response
                    .then()
                    .statusCode(200)
                    .body(containsString((COMPANY_1 + "_" + i)));
            //if response is valid, no need for another inserts
            break;
        }

        Assert.assertTrue("Debezium does not react", i < REPEAT_COUNT);
    }

    //    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        executeUpdate("INSERT INTO COMPANY (name, city) VALUES ('" + COMPANY_2 + "', '" + CITY_2 + "')");
        System.out.println("........................................ insert");

        //validate event in queue
        receiveResponse(200, containsString(COMPANY_2));

        executeUpdate("UPDATE COMPANY SET name = '" + COMPANY_2 + "_changed' WHERE city = '" + CITY_2 + "'");
        System.out.println("........................................ update");

        //validate event with delete is in queue
        receiveResponse(204, is(emptyOrNullString()));
        //validate event with create is in queue
        receiveResponse(200, containsString(COMPANY_2 + "_changed"));
    }

    //    @Test
    @Order(3)
    public void testDelete() throws SQLException {
        int res = executeUpdate("DELETE FROM COMPANY");

        for (int i = 0; i < res; i++) {
            //validate event with delete is in queue
            receiveResponse(204, is(emptyOrNullString()));
        }
    }

    protected Response receiveResponse() {
        return receiveResponse("receive");
    }

    protected Response receiveResponse(String method) {
        return RestAssured.get(getResourcePath() + "/" + method);
    }

    protected void receiveResponse(int statusCode, Matcher<String> stringMatcher) {
        receiveResponse().then()
                .statusCode(statusCode)
                .body(stringMatcher);
    }

    protected int executeUpdate(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

}
