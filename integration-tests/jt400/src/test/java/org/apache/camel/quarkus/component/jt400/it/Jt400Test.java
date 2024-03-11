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
package org.apache.camel.quarkus.component.jt400.it;

import java.util.Locale;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
public class Jt400Test {

    @Test
    public void testDataQueue() {
        RestAssured.given()
                .body("Leonard")
                .post("/jt400/messageQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Leonard"));

        RestAssured.get("/jt400/messageQueue/read")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Leonard"));
    }

    @Test
    public void testKeyedDataQueue() {
        String key = RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body("Sheldon")
                .post("/jt400/keyedDataQueue/write/" + key)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Sheldon"));

        RestAssured.get("/jt400/keyedDataQueue/read/" + key)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Sheldon"));

    }



    //    @Test
    public void testProgramCall() {
        RestAssured.given()
                .body("test")
                .post("/jt400/programCall")
                .then()
                .statusCode(200)
                .body(Matchers.both(Matchers.not(Matchers.containsString("par1"))).and(
                        Matchers.containsString("par2")));
    }

}
