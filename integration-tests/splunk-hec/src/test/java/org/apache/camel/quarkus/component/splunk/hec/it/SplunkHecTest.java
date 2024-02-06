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
package org.apache.camel.quarkus.component.splunk.hec.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.hamcrest.core.StringContains;

@QuarkusTest
@QuarkusTestResource(SplunkTestResource.class)
class SplunkHecTest {

    @Test
    public void produce() throws InterruptedException {

        String url = String.format("http://%s:%d",
                getConfigValue(SplunkConstants.PARAM_REMOTE_HOST, String.class),
                getConfigValue(SplunkConstants.PARAM_REMOTE_PORT, Integer.class));

        RestAssured.given()
                .body("Hello Sheldon")
                .post("/splunk-hec/send")
                .then()
                .statusCode(200);

        //there might a delay between the data written and received by the search, therefore await()
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> RestAssured.given()
                        .request()
                        .formParam("search", "search index=\"testindex\"")
                        .formParam("exec_mode", "oneshot")
                        .relaxedHTTPSValidation()
                        .auth().basic("admin", "password")
                        .post(url + "/services/search/jobs")
                        .then().statusCode(200)
                        .extract().asString(),
                StringContains.containsString("Hello Sheldon"));

    }

    private <T> T getConfigValue(String key, Class<T> type) {
        return ConfigProvider.getConfig().getValue(key, type);
    }
}
