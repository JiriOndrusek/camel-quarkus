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
package org.apache.camel.quarkus.component.leveldb.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LeveldbTest {

    @Test
    public void testAggregate() {
        Map headers = testAggregate(LeveldbRouteBuilder.DIRECT_START,
                Arrays.asList(new String[] { "S", "H", "E", "L", "D", "O", "N" }));

        assertEquals("direct://start", headers.get("fromEndpoint"));
    }

    @Test
    public void testAggregateRecovery() {
        Map headers = testAggregate(LeveldbRouteBuilder.DIRECT_START_WITH_FAILURE,
                Arrays.asList(new String[] { "S", "H", "E", "L", "D", "O", "N" }));

        assertEquals(Boolean.TRUE, headers.get(Exchange.REDELIVERED));
        assertEquals(2, headers.get(Exchange.REDELIVERY_COUNTER));
        assertEquals("direct://startWithFailure", headers.get("fromEndpoint"));

    }

    private Map testAggregate(String path, List<String> messages) {
        return RestAssured.given()
                .queryParam("path", path)
                .contentType(ContentType.JSON)
                .body(messages)
                .post("/leveldb/aggregate")
                .then()
                .statusCode(201)
                .extract().as(Map.class);

    }

    //todo clear db files

}
