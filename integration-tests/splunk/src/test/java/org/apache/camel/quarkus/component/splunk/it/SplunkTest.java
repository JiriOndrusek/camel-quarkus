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
package org.apache.camel.quarkus.component.splunk.it;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.apache.camel.component.splunk.ConsumerType;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SplunkTestResource.class)
class SplunkTest {


    @Test
    public void testNormalSearchWithSubmit() {
        String suffix = "_submitForNormalSearch";

        write(suffix, ProducerType.SUBMIT, 0);

        List<Map<String, String>> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(String.format(
                        "search index=* | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.TEST_INDEX, ProducerType.SUBMIT.name()))
                .post("/splunk/normal")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {
            });

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Irma" + suffix, result.get(0).get("name"));
        Assertions.assertEquals("Earth\"", result.get(0).get("from"));
        Assertions.assertEquals("Leonard" + suffix, result.get(1).get("name"));
        Assertions.assertEquals("Earth 2.0\"", result.get(1).get("from"));
        Assertions.assertEquals("Sheldon" + suffix, result.get(2).get("name"));
        Assertions.assertEquals("Alpha Centauri\"", result.get(2).get("from"));
    }

    @Test
    public void testConsumeRealtime() throws InterruptedException, ExecutionException {
        String suffix = "_streamForRealtime";
        //there is a buffer for stream writing, therefore about 1MB of data has to be written into Splunk

        //data are are written in separated thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        //execute component server to wait for the result
        Future futureResult = executor.submit(
                () -> {
                    for(int i = 0; i < 5000; i++) {
                        write(suffix + i, ProducerType.STREAM, 100);
                    }
                }
                );

        Awaitility.await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(60, TimeUnit.SECONDS).until(
                () -> {
                    String result = RestAssured.get("/splunk/directRealtimePolling")
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    return result.contains(suffix);
                }
        );

        futureResult.cancel(true);
    }

    @Test
    public void testSavedSearchWithTcp() throws InterruptedException {
        String suffix = "_tcpFoSavedSearch";
        //create saved search
        RestAssured.given()
                .baseUri("http://localhost")
                .port(ConfigProvider.getConfig().getValue(SplunkResource.PARAM_REMOTE_PORT, Integer.class))
                .contentType(ContentType.JSON)
                .param("name", SplunkResource.SAVED_SEARCH_NAME)
                .param("disabled", "0")
                .param("description", "descritionText")
                .param("search",
                        "*")
                .post("/services/saved/searches")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        //write data via tcp
        write(suffix, ProducerType.TCP, 0);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(60, TimeUnit.SECONDS).until(
                        () -> {
                            String result = RestAssured.given()
                                    .contentType(ContentType.TEXT)
                                    .body(SplunkResource.SAVED_SEARCH_NAME)
                                    .post("/splunk/savedSearch")
                                    .then()
                                    .statusCode(200)
                                    .extract().asString();


                             return result.contains("Name: Sheldon" + suffix)
                                     && result.contains("Name: Leonard" + suffix)
                                     && result.contains("Name: Irma" + suffix);
                        }
                );

        //remove saved search TODO move to onComplete()
        RestAssured.given()
                .baseUri("http://localhost")
                .port(ConfigProvider.getConfig().getValue(SplunkResource.PARAM_REMOTE_PORT, Integer.class))
                .contentType(ContentType.JSON)
                .delete("/services/saved/searches/" + SplunkResource.SAVED_SEARCH_NAME)
                .then()
                .statusCode(200);
    }

    private void write(String suffix, ProducerType producerType, int lengthOfRandomString) {
        Random random = new Random();
        Consumer<Map> write = data -> RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("index", SplunkTestResource.TEST_INDEX)
                .body(data)
                .post("/splunk/write/" + producerType.name())
                .then()
                .statusCode(201);

        write.accept(CollectionHelper.mapOf("entity", "Name: Sheldon" + suffix + " From: Alpha Centauri", "data", RandomStringUtils.randomAlphanumeric(lengthOfRandomString)));
        write.accept(CollectionHelper.mapOf("entity", "Name: Leonard" + suffix + " From: Earth 2.0", "data", RandomStringUtils.randomAlphanumeric(lengthOfRandomString)));
        write.accept(CollectionHelper.mapOf("entity", "Name: Irma" + suffix + " From: Earth", "data", RandomStringUtils.randomAlphanumeric(lengthOfRandomString)));
    }
}
