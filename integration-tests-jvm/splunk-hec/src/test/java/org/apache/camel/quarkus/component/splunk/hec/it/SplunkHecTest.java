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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(SplunkHecTestResource.class)
class SplunkHecTest {

    @Test
    public void loadComponentSplunkHec() {

        String url = String.format("http://%s:%d",
                getConfigValue(SplunkHecResource.PARAM_REMOTE_HOST, String.class),
                getConfigValue(SplunkHecResource.PARAM_REMOTE_PORT, Integer.class));

        //write data via hec
        RestAssured.given()
                .body("Hello Sheldon!")
                .post("/splunk-hec/send")
                .then()
                .statusCode(200);

        //create search and get its id
        String sid = RestAssured.given().relaxedHTTPSValidation().auth().preemptive().basic("admin", "password")
                .body("search=search *")
                .post(url + "/services/search/jobs")
                .then()
                .contentType(ContentType.XML)
                .extract()
                .path("response.sid");

        //assert response via rest
        RestAssured.given().relaxedHTTPSValidation().auth().preemptive().basic("admin", "password")
                .get(url + "/services/search/jobs/" + sid + "/results")
                .then()
                .body(Matchers.containsString("Hello Sheldon!"));
    }

    private <T> T getConfigValue(String key, Class<T> type) {
        return ConfigProvider.getConfig().getValue(key, type);
    }

}
