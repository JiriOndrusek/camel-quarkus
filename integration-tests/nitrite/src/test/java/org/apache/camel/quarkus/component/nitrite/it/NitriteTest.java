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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(NitriteTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NitriteTest {

    private static final Employee sheldon = new Employee(1L, new GregorianCalendar(2010, 10, 1).getTime(), "Sheldon",
            "Alpha Centauri");

    @Test
    public void repositoryClass() throws CloneNotSupportedException {
        /* Make sure there is no event there before we start inserting */
        RestAssured.get("/nitrite/repositoryClass")
                .then()
                .statusCode(204);

        /* Insert Sheldon */
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sheldon)
                .post("/nitrite/repositoryClass")
                .then()
                .statusCode(200)
                .body("name", is("Sheldon"));
    }

}
