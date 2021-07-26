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
package org.apache.camel.quarkus.component.microprofile.it.faulttolerance;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;

@QuarkusTest
class MicroprofileFaultToleranceTest {

    //@Test
    public void testCamelMicroProfileFaultToleranceFallback() {

        // First request should trigger the fallback response
        RestAssured.post("/microprofile-fault-tolerance/route/faultTolerance")
                .then()
                .statusCode(200)
                .body(Matchers.is(MicroProfileFaultToleranceRoutes.FALLBACK_RESULT));

        // Next request(s) should trigger the expected response
        RestAssured.post("/microprofile-fault-tolerance/route/faultTolerance")
                .then()
                .statusCode(200)
                .body(Matchers.is(MicroProfileFaultToleranceRoutes.RESULT));
    }

    //@Test
    public void testCamelMicroProfileFaultToleranceFallbackWithTimeout() {

        // First request should trigger the fallback response
        RestAssured.given()
                .body("Joe")
                .post("/microprofile-fault-tolerance/route/faultToleranceWithTimeout")
                .then()
                .statusCode(200)
                .body(Matchers.is("Sorry Joe, had to fallback!"));

        // Next request(s) should trigger the expected response
        RestAssured.given()
                .body("Mary")
                .post("/microprofile-fault-tolerance/route/faultToleranceWithTimeout")
                .then()
                .statusCode(200)
                .body(Matchers.is("Regular hi Mary"));
    }

}
