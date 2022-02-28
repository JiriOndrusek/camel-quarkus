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
package org.apache.camel.quarkus.component.quartz.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class QuartzTest {


    @ParameterizedTest()
    @ValueSource(strings = { "cron", "quartz" })
    public void testSchedulerComponent(String component) {
        RestAssured.given()
                .queryParam("fromEndpoint", component)
                .get("/quartz/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus " + component));
    }

    @Test
    public void testProperties() {
        RestAssured.given()
                .queryParam("fromEndpoint", "quartz-properties")
                .get("/quartz/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Quartz Properties"));

//        assertEquals("MyScheduler-" + context.getName(), quartz.getScheduler().getSchedulerName());
//        assertEquals("2", quartz.getScheduler().getSchedulerInstanceId());

    }
}
