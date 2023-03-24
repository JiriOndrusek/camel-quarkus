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
package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

// Tests require restart of Quarkus to avoid persisting of global ssl context.
@QuarkusTest
@TestProfile(CxfSoapWsrmTest.class)
public class CxfSoapWsrmTest implements QuarkusTestProfile {

    // Test is ported from SslTest in Camel-spring-boot/components-starter/camel-cxf-soap-starter
    @Test
    public void testWSRM() {
        RestAssured.given()
                .body("wsrm1")
                .post("/cxf-soap/wsrm")
                .then()
                .statusCode(201)
                .body(equalTo("Hello wsrm1!"));

        //second message will be lost (in the first attempt)
        RestAssured.given()
                .body("wsrm2")
                .post("/cxf-soap/wsrm")
                .then()
                .statusCode(201)
                .body(equalTo("Hello wsrm2!"));
    }

    @Test
    public void testNoWSRM() throws InterruptedException {
        RestAssured.given()
                .body("noWsrm1")
                .post("/cxf-soap/noWsrm")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String body = RestAssured.get("/cxf-soap/noWsrm")
                    .then()
                    .extract().asString();

            return "Hello noWsrm1!".equals(body);
        });

        //second message will be lost (in the first attempt)
        RestAssured.given()
                .body("noWsrm2")
                .post("/cxf-soap/noWsrm")
                .then()
                .statusCode(204);

        //wait some time to get result (which should not be there)
        Thread.sleep(10000);

        RestAssured.get("/cxf-soap/noWsrm")
                .then()
                .statusCode(200)
                .body(not(is("Hello noWsrm2!")));
    }
}
