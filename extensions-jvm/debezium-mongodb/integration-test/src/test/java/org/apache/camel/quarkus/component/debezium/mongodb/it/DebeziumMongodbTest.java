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
package org.apache.camel.quarkus.component.debezium.mongodb.it;

import java.io.IOException;
import java.sql.SQLException;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.bson.Document;
import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(DebeziumMongodbTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMongodbTest {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTest.class);

    protected static String COMPANY_1 = "Best Company";
    protected static String COMPANY_2 = "Even Better Company";
    protected static String CITY_1 = "Prague";
    protected static String CITY_2 = "Paris";

    private static MongoClient mongoClient;

    private static final int EVENTS_COUNT_BEFORE_INSERT = 2;

    private static MongoCollection companies;

    @BeforeAll
    public static void setUp() throws SQLException {
        final String mongoUrl = System.getProperty(DebeziumMongodbResource.PARAM_CLIENT_URL);

        if (mongoUrl != null) {
            mongoClient = MongoClients.create(mongoUrl);
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }
        org.junit.Assume.assumeTrue(mongoClient != null);

        MongoDatabase db = mongoClient.getDatabase("test");

        companies = db.getCollection("companies");
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    @Order(0)
    public void testInit() {
        Response response = null;

        int i = 0;

        //read first x event generated before endpoint was started
        String s;
        boolean init = false;
        do {
            response = RestAssured.get("/debezium-mongodb/receive");

            if (response.getStatusCode() == 204) {
                break;
            }

            s = response.then()
                    .statusCode(200)
                    .extract().asString();
            if (!init && s.contains("init")) {
                init = true;
            }
        } while (++i < EVENTS_COUNT_BEFORE_INSERT && s != null);
    }

    @Test
    @Order(1)
    public void testInsert() {
        Document doc = new Document();
        doc.put("name", COMPANY_1);
        doc.put("city", CITY_1);
        companies.insertOne(doc);

        receiveResponse(200, containsString(COMPANY_1));
    }

    @Test
    @Order(2)
    public void testUpdate() {
        Document doc = new Document().append("name", COMPANY_2).append("city", CITY_2);
        companies.insertOne(doc);

        //validate that event is in queue
        receiveResponse(200, containsString(COMPANY_2));

        Document searchQuery = new Document().append("name", COMPANY_2);
        Document updateQuery = new Document().append("$set", new Document().append("city", CITY_2 + "_changed"));
        companies.updateMany(searchQuery, updateQuery);

        //validate that event for create is in queue
        receiveResponse(200, containsString(CITY_2 + "_changed"));
    }

    @Test
    @Order(3)
    public void testDelete() throws SQLException {
        DeleteResult dr = companies.deleteMany(new Document().append("name", COMPANY_2));
        Assert.assertEquals("Only one company should be deleted.", 1, dr.getDeletedCount());

        //validate that event for delete is in queue
        receiveResponse(200, equalTo("d"), "receiveOperation");
    }

    protected void receiveResponse(int statusCode, Matcher<String> stringMatcher) {
        receiveResponse(statusCode, stringMatcher, "receive");
    }

    protected void receiveResponse(int statusCode, Matcher<String> stringMatcher, String endpoint) {
        RestAssured.get("/debezium-mongodb/" + endpoint).then()
                .statusCode(statusCode)
                .body(stringMatcher);
    }

}
