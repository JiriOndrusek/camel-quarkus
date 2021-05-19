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
package org.apache.camel.quarkus.component.mongodb.it;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
class MongoDbTest {
    public static final String MSG = "Hello Camel Quarkus Mongo DB";
    private static String CAPPED_COLLECTION = "cappedCollection";

    private static MongoClient mongoClient;

    private static MongoDatabase db;


    @BeforeAll
    public static void setUp() throws SQLException {
        final String mongoUrl = "mongodb://" + System.getProperty("quarkus.mongodb.hosts");

        if (mongoUrl != null) {
            mongoClient = MongoClients.create(mongoUrl);
        }
        org.junit.Assume.assumeTrue(mongoClient != null);

        db = mongoClient.getDatabase("test");
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { MongoDbResource.DEFAULT_MONGO_CLIENT_NAME, MongoDbResource.NAMED_MONGO_CLIENT_NAME })
    public void testMongoDbComponent(String namedClient) {
        // As we will create a route for each client, we use a different collection for each route
        String collectionName = String.format("%sCamelTest", namedClient);

        // Write to collection
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", namedClient)
                .post("/mongodb/collection/" + collectionName)
                .then()
                .statusCode(201);

        // Retrieve from collection
        JsonPath jsonPath = RestAssured
                .given()
                .header("mongoClientName", namedClient)
                .get("/mongodb/collection/" + collectionName)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        List<Map<String, String>> documents = jsonPath.get();
        assertEquals(1, documents.size());

        Map<String, String> document = documents.get(0);
        assertEquals(MSG, document.get("message"));
    }

    @Test
    public void testDynamicOperation() {
        String collectionName = "dynamicCamelTest";

        // Write to collection
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .post("/mongodb/collection/dynamic/" + collectionName)
                .then()
                .statusCode(200)
                .body("message", is(MSG));

        //count results with dynamic operation
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .header("dynamicOperation", "count")
                .post("/mongodb/collection/dynamic/" + collectionName)
                .then()
                .statusCode(200)
                .body(is("1"));

    }


    @Test
    public void testTailingConsumer() throws Exception {

        db.getCollection(CAPPED_COLLECTION).drop();
        MongoCollection cappedTestCollection;
        db.createCollection(CAPPED_COLLECTION,
                new CreateCollectionOptions().capped(true).sizeInBytes(1000000000).maxDocuments(100));
        cappedTestCollection = db.getCollection(CAPPED_COLLECTION, Document.class);


        RestAssured
                .given()
                .get("/mongodb/startRoute/tailing/")
                .then()
                .statusCode(204);

        for (int i = 1; i <= 1000; i++) {
            cappedTestCollection.insertOne(new Document("increasing", i).append("string", "value" + i));
        }

        Thread.sleep(1000);

    }

}
