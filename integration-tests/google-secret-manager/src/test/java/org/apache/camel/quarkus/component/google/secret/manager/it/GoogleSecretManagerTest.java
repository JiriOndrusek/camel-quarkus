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
package org.apache.camel.quarkus.component.google.secret.manager.it;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import io.quarkus.logging.Log;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerConstants;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerOperations;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(GoogleSecretManagerTestResource.class)
@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+"),
        @EnabledIfEnvironmentVariable(named = "GOOGLE_PROJECT_ID", matches = ".+")
})
class GoogleSecretManagerTest {

    private GooglePubSubCustomizer customizer;

    @Test
    void secretCreateListDelete() {
        final String secretToCreate = "firstSecret!";
        final String secretId = "CQTestSecret" + System.currentTimeMillis();
        String createdName;

        boolean deleted = false;

        try {
            //create secret
            createdName = createSecret(secretId, secretToCreate);
            assertTrue(createdName.contains(secretId));

            //parse the name without /version/...
            String name = createdName.substring(0, createdName.indexOf("/version"));
            String version = createdName.substring(createdName.lastIndexOf("/") + 1);

            //get secret
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(GoogleSecretManagerConstants.SECRET_ID, secretId, GoogleSecretManagerConstants.VERSION_ID,
                            version))
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.getSecretVersion)
                    .then()
                    .statusCode(200)
                    .body(is(secretToCreate));

            // list secrets
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.listSecrets)
                    .then()
                    .statusCode(200)
                    .body(containsString(name));

            //delete secret
            deleteSecret(secretId);

            //verify that the secret is gone
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.listSecrets)
                    .then()
                    .statusCode(200)
                    .body(not(containsString(name)));

            deleted = true;

        } finally {
            if (!deleted && !MockBackendUtils.startMockBackend(false)) {
                String file = ConfigProvider.getConfig().getValue("cq.google-secrets-manager.path-to-service-account-key",
                        String.class);
                String projectName = ConfigProvider.getConfig().getValue("cq.google-secrets-manager.project-name",
                        String.class);
                GoogleSecretManagerTestResource.deleteSecret(secretId, file, projectName);
            }
        }
    }

    @Test
    void loadGcpSecretAndRefreshTest() throws Exception {
        InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
                record -> record.getMessage().contains("Reloading CamelContext"));
        LogManager.getLogManager().getLogger("").addHandler(inMemoryLogHandler);

        String expectedSecret = ConfigProvider.getConfig().getValue("gcpSecretValue", String.class);
        String secretId = ConfigProvider.getConfig().getValue("gcpSecretId", String.class);
        String projectId = ConfigProvider.getConfig().getValue("cqProjectId", String.class);
        String accessFile = ConfigProvider.getConfig().getValue("gcpAccessFile", String.class);

        //verify default secret value
        RestAssured
                .get("/google-secret-manager/getGcpSecret/")
                .then()
                .statusCode(200)
                .body(is(expectedSecret));

        //change secret
        GoogleSecretManagerTestResource.updateSecret(secretId, "new_changeit", accessFile, projectId);

        //wait a moment and verify that the secret returned by the route is not changed
        Thread.sleep(20000);

        RestAssured
                .get("/google-secret-manager/getGcpSecret/")
                .then()
                .statusCode(200)
                .body(is(expectedSecret));

        //simulate that secret change is detected and proper message to a subscription is sent
        customizer.sendMsg("mocked message forcing refresh", Map.of("eventType", "SECRET_UPDATE", "secretId", secretId));

        //wait till the refresh is executed, route should return the new secret
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured
                        .get("/google-secret-manager/getGcpSecret/")
                        .then()
                        .statusCode(200)
                        .body(is("new_changeit")));
    }

    protected String createSecret(String secretName, String secretValue) {
        String createdArn = Awaitility.await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    try {
                        return RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(Collections.singletonMap(GoogleSecretManagerConstants.SECRET_ID, secretName))
                                .queryParam("body", secretValue)
                                .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.createSecret)
                                .then()
                                .statusCode(200)
                                .extract().asString();
                    } catch (Exception e) {
                        return null;
                    }
                }, Objects::nonNull);

        return createdArn;
    }

    protected void deleteSecret(String secretId) {
        if (secretId != null) {
            Log.info("Deleting secret: " + secretId);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(GoogleSecretManagerConstants.SECRET_ID, secretId))
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.deleteSecret)
                    .then()
                    .statusCode(200)
                    .body(CoreMatchers.is("true"));
        }
    }

    public void setCustomizer(GooglePubSubCustomizer customizer) {
        this.customizer = customizer;
    }
}
