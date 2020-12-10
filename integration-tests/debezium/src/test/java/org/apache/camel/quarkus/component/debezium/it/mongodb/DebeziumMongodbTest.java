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
package org.apache.camel.quarkus.component.debezium.it.mongodb;

import java.sql.Connection;
import java.sql.SQLException;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.component.debezium.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.it.Type;
import org.bson.Document;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(DebeziumMongodbTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMongodbTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTest.class);

    //has to be constant and has to be equal to Type.mongodb.getJdbcProperty
    public static final String PROPERTY_JDBC = "mongodb_jdbc";

    private static MongoClient mongoClient;

    private static MongoCollection companies;

    public DebeziumMongodbTest() {
        super(Type.mongodb);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        final String mongoUrl = System.getProperty(Type.mongodb.getPropertyJdbc());

        if (mongoUrl != null) {
            mongoClient = MongoClients.create(mongoUrl);
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }

        org.junit.Assume.assumeTrue(mongoClient != null);

        MongoDatabase db = mongoClient.getDatabase("test");

        companies = db.getCollection("companies");
    }

    @Before
    public void before() {
        org.junit.Assume.assumeTrue(mongoClient != null);
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    protected Connection getConnection() {
        return null;
    }

    @Override
    protected String getCompanyTableName() {
        return "Test." + super.getCompanyTableName();
    }

    @Test
    @Order(0)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testReceiveInit() {
        receiveResponse()
                .then()
                .statusCode(200)
                .body(containsString("init"));
    }

    @Override
    protected void insertCompany(String name, String city) throws SQLException {
        Document doc = new Document();
        doc.put("name", name);
        doc.put("city", city);
        companies.insertOne(doc);
    }

    @Override
    protected void inInitialized(String s) {
        Assert.assertNotNull(s, mongoClient);
    }

    @Test
    @Order(1)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testInsert() throws SQLException {
        super.testInsert();
    }

    @Test
    @Order(2)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testUpdate() throws SQLException {

    }

    @Test
    @Order(3)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testDelete() throws SQLException {

    }

}
