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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
@QuarkusTest
class AzureKeyVaultTest extends AbstractKeyVaultContextRefreshTest {

    @Override
    String generateSecretName() {
        return "cq-secret-context-refresh-clientId-" + UUID.randomUUID();
    }

    @Test
    void secretCreateRetrieveDeletePurge() {
        String secretName = UUID.randomUUID().toString();
        String secret = "Hello Camel Quarkus Azure Key Vault";

        try {
            // Create secret
            RestAssured.given()
                    .body(secret)
                    .post("/azure-key-vault/secret/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secretName));

            // Retrieve secret
            RestAssured.given()
                    .get("/azure-key-vault/secret/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secret));
        } finally {
            deleteSecretImmediately(secretName);
        }
    }

    @Test
    void propertyPlaceholder() {
        String secretName = "camel-quarkus-secret";
        String secret = "Hello Camel Quarkus Azure Key Vault From Property Placeholder";

        try {
            // Create secret
            RestAssured.given()
                    .body(secret)
                    .post("/azure-key-vault/secret/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secretName));

            // Retrieve secret
            RestAssured.given()
                    .get("/azure-key-vault/secret/from/placeholder")
                    .then()
                    .statusCode(200)
                    .body(is(secret));
        } finally {
            deleteSecretImmediately(secretName);
        }
    }

    @EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_BLOB_CONTAINER_NAME", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_CONNECTION_STRING", matches = ".+")
    @Test
    void testContextRefresh() {
        super.contextRefresh();
    }
}
