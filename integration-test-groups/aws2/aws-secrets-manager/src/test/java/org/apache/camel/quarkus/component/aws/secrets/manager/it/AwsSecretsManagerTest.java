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
package org.apache.camel.quarkus.component.aws.secrets.manager.it;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)

public class AwsSecretsManagerTest extends BaseAWs2TestSupport {

    public AwsSecretsManagerTest() {
        super("/aws-secrets-manager");
    }

    /**
     * @param useHeaders If true, use headers instead of uri parts.
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testOperations(boolean useHeaders) {
        final String secretToCreate = "loadFirst";
        final String secret2ToCreate = "changeit2";
        final String secretToUpdate = "loadSecond";
        final String nameToCreate = "CQTestSecret-operation-" + useHeaders + "-1-" + System.currentTimeMillis();
        final String name2ToCreate = "CQTestSecret2-operation-" + useHeaders + "-2-" + System.currentTimeMillis();
        final String description2ToCreate = "description-" + name2ToCreate;
        String createdArn = null;
        String createdArn2 = null;

        try {
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            Map<String, Object> headers = useHeaders
                    ? Map.of(SecretsManagerConstants.OPERATION, SecretsManagerOperations.createSecret,
                            SecretsManagerConstants.SECRET_NAME, name2ToCreate,
                            SecretsManagerConstants.SECRET_DESCRIPTION, description2ToCreate)
                    : Collections.singletonMap(SecretsManagerConstants.SECRET_NAME, name2ToCreate);
            createdArn2 = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(headers)
                    .queryParam("body", secret2ToCreate)
                    .queryParam("useHeaders", useHeaders)
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.createSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertNotNull(createdArn);

            final String finalCreatedArn = createdArn;
            final String finalCreatedArn2 = createdArn2;
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);
                        // contains both created secrets
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        assertTrue(secrets.containsKey(finalCreatedArn2));
                    });
            if (useHeaders) {
                //use MAX_RESULTS header
                Awaitility.await().pollDelay(5, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES)
                        .untilAsserted(
                                () -> {
                                    Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(1);
                                    // contains both created secrets
                                    assertTrue(secrets.size() == 1);
                                });
            }

            String secret = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertEquals(secretToCreate, secret);

            Map description = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.describeSecret)
                    .then()
                    .statusCode(201)
                    .extract().as(Map.class);

            assertEquals(3, description.size());

            assertEquals(true, description.get("sdkHttpSuccessful"));
            assertEquals(name2ToCreate, description.get("name"));
            if (useHeaders) {
                assertEquals(description2ToCreate, description.get("description"));
            }

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.deleteSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);
                        // by default secrets marked for deletion are not listed (can be enabled with https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/secretsmanager/model/ListSecretsRequest.Builder.html#includePlannedDeletion(java.lang.Boolean))
                        // but on localstack they are present (with non-null deletedDate field) - see https://github.com/localstack/localstack/issues/11635
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        if (!MockBackendUtils.startMockBackend(false)) {
                            assertFalse(secrets.containsKey(finalCreatedArn2));
                        } else {
                            assertFalse(secrets.get(finalCreatedArn));
                            assertTrue(secrets.get(finalCreatedArn2));
                        }
                    });

            // operation rotateSecret fails on local stack with 500 when upgraded to 2.2.0
            // it needs lambda function ARN to work
            // TODO:See https://github.com/apache/camel-quarkus/issues/5300

            //  RestAssured.given()
            //  .contentType(ContentType.JSON)
            //  .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
            //  .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.rotateSecret)
            //  .then()
            //  .statusCode(201)
            //  .body(is("true"));

            AwsSecretsManagerUtil.updateSecret(createdArn, secretToUpdate);

            String updatedSecret = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertEquals(secretToUpdate, updatedSecret);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.restoreSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);

                        //none of them is deleted, because they were restored
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        assertTrue(secrets.containsKey(finalCreatedArn2));
                    });

            // operation replicateSecretToRegions fails on local stack with 500
            // There is no possibility to delete secret in different region via camel

            // find different region then the actually used
            //  String anotherRegion = regions().stream().filter(region -> !region.equals(region())).findFirst().get();
            // RestAssured.given()
            // .contentType(ContentType.JSON)
            // .body(CollectionHelper.mapOf(SecretsManagerConstants.SECRET_ID, createdArn2,
            // SecretsManagerConstants.SECRET_REPLICATION_REGIONS, anotherRegion))
            // .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.replicateSecretToRegions)
            // .then()
            // .statusCode(201)
            // .body(is("true"));
        } finally {
            // we must clean created secrets
            // also on localstack, if not the second run of operations would fail
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn2);
        }
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        final String secretToCreate = "loadFirst";
        final String nameToCreate = "CQTestSecret-provider-" + System.currentTimeMillis();
        String createdArn = null;

        try {
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

        } finally {
            // we must clean created secrets
            // also on localstack, if not the second run of operations would fail
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
        }
    }

    @Test
    public void testAwsSecretRefreshPeriodicTaskExists() {
        RestAssured.get("/aws-secrets-manager/period/task/resolver/exists")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    // https://issues.apache.org/jira/browse/CAMEL-21330
    @EnabledIf(MockBackendDisabled.class)
    public void testPropertyFunction() {
        String createdArn = null;
        final String secretToCreate = "loadFirst";
        final String secretToUpdate = "loadSecond";
        try {
            final String nameToCreate = "CQTestSecretPropFunction" + System.currentTimeMillis();
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_FIRST));

            AwsSecretsManagerUtil.updateSecret(createdArn, secretToUpdate);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_SECOND));
        } finally {
            if (!MockBackendUtils.startMockBackend(false)) {
                // we must clean created secrets
                // skip cleaning on localstack
                AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
            }
        }
    }
}
