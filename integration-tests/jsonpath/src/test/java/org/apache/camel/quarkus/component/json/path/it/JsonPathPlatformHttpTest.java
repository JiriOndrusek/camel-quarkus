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
package org.apache.camel.quarkus.component.json.path.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class JsonPathPlatformHttpTest {

    public static final String BODY = "{room:{temperature:30}}";
    public static final String RESULT = "HOT";

    @Test
    public void testWithPlatformHttp() {
        String result = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .post("/getTemperature")
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals(RESULT, result);
    }

    @Test
    public void testWithPlatformHttp2() {
        String result = RestAssured.given()
                //                .contentType(ContentType.JSON)
                .body(BODY)
                .post("/getTemperature2")
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals(RESULT, result);
    }

    @Test
    public void testWithoutPlatformHttp() {
        String result = RestAssured.given()
                .body("30")
                .when().post("/jsonpath/getTemperature")
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals(RESULT, result);
    }

}
