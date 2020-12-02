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

import java.sql.Connection;
import java.sql.SQLException;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.bson.Document;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@QuarkusTestResource(DebeziumMongodbTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMongodbTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTest.class);

    //has to be constant and has to be equal to Type.mongodb.getJdbcProperty
    public static final String PROPERTY_JDBC = "mongodb_jdbc";

    private static MongoClient mongoClient;

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

        //create companies table
        MongoDatabase db = mongoClient.getDatabase("test");
        db.createCollection("companies");
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
    protected boolean isInitialized() {
        return mongoClient != null;
    }

    @Override
    protected String getCompanyTableName() {
        return "Test." + super.getCompanyTableName();
    }

    @Test
    @Order(0)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testReceiveInitCompany() {
        MongoDatabase db = mongoClient.getDatabase("test");

        MongoCollection companies = db.getCollection("companies");
        Document doc = new Document();
        doc.put("name", "company01");
        companies.insertOne(doc);

        Response response = receiveResponse();
        System.out.println(response.getStatusCode());
    }

    @Override
    protected void insertCompany(int i) throws SQLException {
        MongoDatabase db = mongoClient.getDatabase("test");

        MongoCollection companies = db.getCollection("companies");
        Document doc = new Document();
        doc.put("name", COMPANY_1 + "_" + i);
        doc.put("city", CITY_1);
        companies.insertOne(doc);
    }

    //fromexample
    //debezium-mongodb:dbz-test-1?offsetStorageFileName=/usr/offset-file-1.dat&mongodbHosts=rs0/localhost:27017&mongodbUser=debezium&mongodbPassword=dbz&mongodbName=dbserver1&databaseHistoryFileName=/usr/history-file-1.dat
    //fom debug
    //debezium-mongodb:localhost?offsetStorageFileName=/tmp/DebeziumMongodbTestResource-store-2800689676957064560&mongodbPassword=changeit&mongodbName=docker-rs&mongodbHosts=localhost:32821

    @Test
    @Order(2)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testUpdate() throws SQLException {
        super.testUpdate();
    }

    @Test
    @Order(3)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testDelete() throws SQLException {
        super.testDelete();
    }

}
