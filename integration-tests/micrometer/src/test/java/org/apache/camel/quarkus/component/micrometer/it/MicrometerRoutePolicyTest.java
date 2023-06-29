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
package org.apache.camel.quarkus.component.micrometer.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MicrometerRoutePolicyProfile.class)
class MicrometerRoutePolicyTest extends AbstractMicrometerTest {

    @Test
    public void testMicrometerRoutePolicyFactory() {
        RestAssured.get("/micrometer/timer")
                .then()
                .statusCode(200);
        assertTrue(
                getMetricValue(Integer.class, "counter", "camel.exchanges.succeeded", "routeId=micrometer-metrics-timer") > 0);
        assertEquals(0, getMetricValue(Integer.class, "counter", "camel.exchanges.failed", "routeId=micrometer-metrics-timer"));
    }
}
