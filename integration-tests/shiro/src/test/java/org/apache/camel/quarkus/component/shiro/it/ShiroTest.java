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
package org.apache.camel.quarkus.component.shiro.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShiroTest {

    private static ShiroSecurityToken CORRECT_TOKEN = new ShiroSecurityToken("sheldon", "earth2");
    private static ShiroSecurityToken WRONG_TOKEN = new ShiroSecurityToken("sheldon", "wrong");

    @Test
    public void testHeaders() {
        testCorrectAndWrongAccess("headers");
    }

    @Test
    public void testToken() {
        testCorrectAndWrongAccess("token");
    }

    @Test
    public void testBase64() {
        testCorrectAndWrongAccess("token");
    }

    private void testCorrectAndWrongAccess(String path) {

        RestAssured.given()
                .queryParam("expectSuccess", true)
                .contentType(ContentType.JSON)
                .body(CORRECT_TOKEN)
                .post("/shiro/" + path)
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("expectSuccess", false)
                .contentType(ContentType.JSON)
                .body(WRONG_TOKEN)
                .post("/shiro/" + path)
                .then()
                .statusCode(204);
    }

}
