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
package org.apache.camel.quarkus.component.nitrite.it;

import java.util.GregorianCalendar;
import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.dizitart.no2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

@QuarkusTest
@QuarkusTestResource(NitriteTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NitriteTest {

    private static final String INSERT = "INSERT";
    private static final String UPDATE = "UPDATE";
    private static final String REMOVE = "REMOVE";
    private Employee sheldon = new Employee(1L, new GregorianCalendar(2010, 10, 1).getTime(), "Sheldon", "Alpha Centauri");
    private Employee leonard = new Employee(2L, new GregorianCalendar(2015, 10, 1).getTime(), "Leonard", "Earth");
    private Employee irma = new Employee(3L, new GregorianCalendar(2011, 10, 1).getTime(), "Irma", "Jupiter");

    @Test
    @Order(1)
    public void testRepositoryInsert() {
        //initialize consumer for Employee
        get("getEmployee").then().statusCode(204);
        //initialize consumer
        get("getEmployee").then().statusCode(204);

        insert(sheldon, "insertEmployee"); //insert employee sheldon
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(sheldon.getName()), containsString(INSERT));

        insert(leonard, "insertEmployee");
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(leonard.getName()), containsString(INSERT));

        insert(irma, "insertEmployee");
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(irma.getName()), containsString(INSERT));
    }

    @Test
    @Order(2)
    public void testRepositoryUpdate() throws Exception {
        Employee updatedSheldon = (Employee) sheldon.clone();
        updatedSheldon.setAddress("Earth2.0");
        operationEmployee(Operation.Type.update, "name", "Sheldon", updatedSheldon);

        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase("Earth2.0"), containsString(UPDATE));
    }

    @Test
    @Order(3)
    public void testRepositoryFind() throws Exception {
        List results = (List) operationEmployee(Operation.Type.find, "address", "Earth", null);
        Assertions.assertEquals(1, results.size(), "After update, there is 1 employee from Earth");
        results = (List) operationEmployee(Operation.Type.findGt, "empId", 0, null);
        Assertions.assertEquals(3, results.size(), "After update, there are 3 employees at all");
    }

    @Test
    @Order(4)
    public void testRepositoryDelete() {
        operationEmployee(Operation.Type.delete, "address", "Earth", null);
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(leonard.getName()), containsString("REMOVE"));
        operationEmployee(Operation.Type.delete, "address", "Earth2.0", null);
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(sheldon.getName()), containsString("REMOVE"));
        operationEmployee(Operation.Type.delete, "address", "Jupiter", null);
        get("getEmployee").then().statusCode(200).body(containsStringIgnoringCase(irma.getName()), containsString("REMOVE"));

        List results = (List) operationEmployee(Operation.Type.findGt, "empId", "0", null);
        Assertions.assertTrue(results.isEmpty(), "After deletion, there are no employees");
    }

    @Test
    @Order(5)
    public void testCollectionInsert() throws Exception {
        //initialize customer for collection
        get("getCollection").then().statusCode(204);

        insert(Document.createDocument("key1", "value1"), "insertCollection");
        get("getCollection").then().statusCode(200).body(containsStringIgnoringCase("key1"), containsString(INSERT));

        insert(Document.createDocument("key2", "value2"), "insertCollection");
        get("getCollection").then().statusCode(200).body(containsStringIgnoringCase("key2"), containsString(INSERT));
    }

    @Test
    @Order(6)
    public void testCollectionUpdate() {

        get("getCollection").then().statusCode(204);
        operationCollection(Operation.Type.insert, null, null, Document.createDocument("key1", "value_beforeUpdate"));
        get("getCollection").then().statusCode(200).body(containsStringIgnoringCase("value_beforeUpdate"),
                containsString(INSERT));

        operationCollection(Operation.Type.update, "key1", "value_beforeUpdate",
                Document.createDocument("key1", "value_afterUpdate"));
        get("getCollection").then().statusCode(200).body(containsStringIgnoringCase("value_afterUpdate"),
                containsString(UPDATE));
    }

    @Test
    @Order(7)
    public void testCollectionRemove() {
        operationCollection(Operation.Type.delete, "key1", "value1", null);
        get("getCollection").then().statusCode(200).body(containsStringIgnoringCase("key1"), containsString("REMOVE"));
    }

    @Test
    @Order(8)
    public void testCollectionFind() throws Exception {
        List results = (List) operationCollection(Operation.Type.find, "key1", "value_afterUpdate", null);
        Assertions.assertEquals(1, results.size(), "There is only 1 item with value1");
    }

    private static Response get(String uri) {
        return RestAssured.get("/nitrite/" + uri);
    }

    private void insert(Object object, String uri) {
        RestAssured.given() //
                .contentType(ContentType.JSON)
                .body(object)
                .post("/nitrite/" + uri)
                .then()
                .statusCode(200);
    }

    private Object operationEmployee(Operation.Type type, String field, Object value, Employee employee) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(type, field, value, employee))
                .post("/nitrite/operationEmployee")
                .then()
                .extract().as(Object.class);
    }

    private Object operationCollection(Operation.Type type, String field, Object value, Document document) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(type, field, value, document))
                .post("/nitrite/operationCollection")
                .then()
                .extract().as(Object.class);
    }

}
