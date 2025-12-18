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
package org.apache.camel.quarkus.component.cyberark.vault.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CyberarkVaultWireMockTestResource.class)
class CyberarkVaultTest {

    private static final Logger LOG = LoggerFactory.getLogger(CyberarkVaultTest.class);

    private static HttpClient httpClient;
    private static String authToken;

    //    @BeforeAll
    public static void setupSecrets() throws Exception {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Authenticate
        String url = String.format("%s/authn/%s/%s/authenticate",
                System.getProperty("camel.cyberark.url"),
                System.getProperty("camel.cyberark.account"),
                System.getProperty("camel.cyberark.username"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(System.getProperty("camel.cyberark.apiKey")))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        authToken = response.body();

        // Create test secrets
        createSecret("test/secret", "mySecretValue");
        createSecret("production/database", "{\"username\":\"prod-user\",\"password\":\"prod-pass\"}");
    }

    @Test
    public void testRetrieveSecret() throws Exception {

        String secret = UUID.randomUUID().toString();

        //create secret
        RestAssured.given()
                .body(secret)
                .post("/cyberark-vault/createSecret/false")
                .then()
                .statusCode(500)
                .body(containsString("403"));

        //create secret
        RestAssured.given()
                .body(secret)
                .post("/cyberark-vault/createSecret/true")
                .then()
                .statusCode(200);

        //verify secret value
        RestAssured
                .get("/cyberark-vault/getSecret/")
                .then()
                .statusCode(200)
                .body(is(secret));
    }

    private static void createSecret(String secretId, String secretValue) {
        try {
            String url = String.format("%s/secrets/%s/variable/%s",
                    System.getProperty("camel.cyberark.url"),
                    System.getProperty("camel.cyberark.account"),
                    secretId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Token token=\"" + Base64.getEncoder()
                            .encodeToString(authToken.getBytes(StandardCharsets.UTF_8)) + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(secretValue))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("Created secret '{}': HTTP {}", secretId, response.statusCode());
        } catch (Exception e) {
            LOG.warn("Could not create secret '{}': {}", secretId, e.getMessage());
        }
    }
}
