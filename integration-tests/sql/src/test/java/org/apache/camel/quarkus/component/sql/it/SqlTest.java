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
package org.apache.camel.quarkus.component.sql.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
class SqlTest {

    @Test
    public void testSqlComponent() {
        // Create Camel species
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("table", "camel")
                .body(CollectionHelper.mapOf("species", "Dromedarius"))
                .post("/sql/insert")
                .then()
                .statusCode(201);

        // Retrieve camel species as map
        RestAssured.get("/sql/get/Dromedarius")
                .then()
                .statusCode(200)
                .body(is("[{ID=1, SPECIES=Dromedarius}]"));

        // Retrieve camel species as list
        RestAssured.get("/sql/get/Dromedarius/list")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));

        // Retrieve camel species as type
        RestAssured.get("/sql/get/Dromedarius/list/type")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));
    }

    @Test
    public void testSqlStoredComponent() {
        // Invoke ADD_NUMS stored procedure
        RestAssured.given()
                .queryParam("numA", 10)
                .queryParam("numB", 5)
                .get("/sql/storedproc")
                .then()
                .statusCode(200)
                .body(is("15"));
    }

    @Test
    public void testConsumer() throws InterruptedException {
        testConsumer(1, "consumerRoute");
    }

    @Test
    public void testClasspathConsumer() throws InterruptedException {
        testConsumer(2, "consumerClasspathRoute");
    }

    @Test
    public void testFileConsumer() throws InterruptedException {
        testConsumer(3, "consumerFileRoute");
    }

    private void testConsumer(int id, String routeId) throws InterruptedException {
        RestAssured.given()
                .get("/sql/route/" + routeId + "/start")
                .then().statusCode(204);

        //wait for consumer rto start
        Thread.sleep(500);

        Map project = CollectionHelper.mapOf("ID", id, "PROJECT", routeId, "LICENSE", "222", "PROCESSED", false);
        Map updatedProject = CollectionHelper.mapOf("ID", id, "PROJECT", routeId, "LICENSE", "XXX", "PROCESSED", false);

        RestAssured.given()
                .queryParam("table", "projects")
                .contentType(ContentType.JSON)
                .body(project)
                .post("/sql/insert")
                .then()
                .statusCode(201);

        //wait for the record to be caught
        Thread.sleep(500);

        RestAssured.get("/sql/get/results/" + routeId)
                .then()
                .statusCode(200)
                .body("size()", is(1), "$", hasItem(project));

        //update
        RestAssured.given()
                .queryParam("table", "projects")
                .contentType(ContentType.JSON)
                .body(updatedProject)
                .post("/sql/update")
                .then()
                .statusCode(201);

        Thread.sleep(500);

        RestAssured.get("/sql/get/results/" + routeId)
                .then()
                .statusCode(200)
                .body("size()", is(1), "$", hasItem(updatedProject));

        RestAssured.given()
                .get("/sql/route/" + routeId + "/stop")
                .then().statusCode(204);

        Thread.sleep(500);
    }
}
