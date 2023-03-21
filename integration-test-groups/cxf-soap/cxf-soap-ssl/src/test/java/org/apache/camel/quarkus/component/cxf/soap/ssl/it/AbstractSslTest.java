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
package org.apache.camel.quarkus.component.cxf.soap.ssl.it;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Each of global tests have to restart the quarkus.
 * If quarkus is not restarted - via TestProfile - some values in the context would stay and would fail the second test
 * execution.
 */
public abstract class AbstractSslTest implements QuarkusTestProfile {

    void testInvoke(boolean global, boolean trust) throws Exception {

        String url = String.format("/cxf-soap/ssl/%s/%s", trust ? "trusted" : "untrusted", global);

        ValidatableResponse resp = RestAssured.given()
                .body("ssl")
                .post(url)
                .then();

        if (trust) {
            resp.statusCode(201)
                    .body(equalTo("Hello ssl!"));
        } else {
            resp.statusCode(500)
                    .body(containsString("signature check failed"));
        }
    }

    @Override
    public String getConfigProfile() {
        return getClass().getSimpleName();
    }
}
