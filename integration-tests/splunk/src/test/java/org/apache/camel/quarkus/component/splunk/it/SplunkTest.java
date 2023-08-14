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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SplunkTestResource.class)
class SplunkTest {


    @Test
    public void testNormalSearchWithSubmit() {
        String suffix = "_normalSearchOfSubmit";

        write(suffix, ProducerType.SUBMIT, 0);

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(
                () -> {
                    String result = RestAssured.given()
                            .get("/splunk/results/normalSearch")
                            .then()
                            .statusCode(200)
                            .extract().asString();


                    return result.contains("Name: Sheldon" + suffix)
                            && result.contains("Name: Leonard" + suffix)
                            && result.contains("Name: Irma" + suffix);
                }
        );
    }

    @Test
    public void testStreamForRealtime() throws InterruptedException, ExecutionException {
        String suffix = "_RealtimeSearchOfStream";
        //there is a buffer for stream writing, therefore about 1MB of data has to be written into Splunk

        //data are written in separated thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        //execute component server to wait for the result
        Future futureResult = executor.submit(
                () -> {
                    for(int i = 0; i < 5000; i++) {
                        write(suffix + i, ProducerType.STREAM, 100);
                    }
                }
                );

        try {
            Awaitility.await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(60, TimeUnit.SECONDS).until(
                    () -> {
                        String result = RestAssured.get("/splunk/results/realtimeSearch")
                                .then()
                                .statusCode(200)
                                .extract().asString();

                        return result.contains(suffix);
                    }
            );
        } finally {
            futureResult.cancel(true);
        }
    }

    @Test
    public void testSavedSearchWithTcp() throws InterruptedException {
        String suffix = "_SavedSearchOfTcp";
        //create saved search
        RestAssured.given()
                .baseUri("http://localhost")
                .port(ConfigProvider.getConfig().getValue(SplunkResource.PARAM_REMOTE_PORT, Integer.class))
                .contentType(ContentType.JSON)
                .param("name", SplunkResource.SAVED_SEARCH_NAME)
                .param("disabled", "0")
                .param("description", "descritionText")
                .param("search",
                        "search index=* sourcetype=\"TCP\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"")
                .post("/services/saved/searches")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        //start route
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(60, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.given()
                                .get("/splunk/start/route/savedSearchRoute/")
                                .then()
                                .statusCode(200)
                                .extract().body().as(Boolean.class));


        //write data via tcp
        write(suffix, ProducerType.TCP, 0);

        Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).atMost(60, TimeUnit.SECONDS).until(
                        () -> {
                            String result = RestAssured.given()
                                    .get("/splunk/results/savedSearch")
                                    .then()
                                    .statusCode(200)
                                    .extract().asString();


                             return result.contains("Name: Sheldon" + suffix)
                                     && result.contains("Name: Leonard" + suffix)
                                     && result.contains("Name: Irma" + suffix);
                        }
                );

//        //remove saved search TODO move to onComplete()
//        RestAssured.given()
//                .baseUri("http://localhost")
//                .port(ConfigProvider.getConfig().getValue(SplunkResource.PARAM_REMOTE_PORT, Integer.class))
//                .contentType(ContentType.JSON)
//                .delete("/services/saved/searches/" + SplunkResource.SAVED_SEARCH_NAME)
//                .then()
//                .statusCode(200);
    }

    private void write(String suffix, ProducerType producerType, int lengthOfRandomString) {
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
