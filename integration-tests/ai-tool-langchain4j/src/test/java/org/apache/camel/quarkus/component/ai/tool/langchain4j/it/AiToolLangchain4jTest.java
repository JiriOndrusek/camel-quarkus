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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class AiToolLangchain4jTest {

    @Test
    void aiServiceShouldInvokeCamelToolAndReturnResult() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/chat")
                .then()
                .statusCode(200)
                .body(containsString("Prague"))
                .body(containsString("Sunny"));
    }

    @Test
    void weatherTagShouldExposeOnlyWeatherTools() {
        RestAssured.given()
                .get("/ai-tool-langchain4j/tools/weather")
                .then()
                .statusCode(200)
                .body(containsString("getWeather"))
                .body(not(containsString("getNews")));
    }

    @Test
    void adminTagShouldExposeOnlyAdminTools() {
        RestAssured.given()
                .get("/ai-tool-langchain4j/tools/admin")
                .then()
                .statusCode(200)
                .body(containsString("getNews"))
                .body(not(containsString("getWeather")));
    }

    @Test
    void adminServiceShouldInvokeAdminToolAndReturnResult() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What are the latest news about camel?")
                .post("/ai-tool-langchain4j-admin/chat")
                .then()
                .statusCode(200)
                .body(containsString("camel"))
                .body(containsString("news"));
    }
}
