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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SplunkTestResource.class)
class SplunkTest {
    // Test matrix
    static Stream<Arguments> testMatrix() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        argumentBuilder.add(Arguments.of(ConsumerType.NORMAL, ProducerType.STREAM, true));
        argumentBuilder.add(Arguments.of(ConsumerType.REALTIME, ProducerType.SUBMIT, false));
        argumentBuilder.add(Arguments.of(ConsumerType.SAVEDSEARCH, ProducerType.TCP, false));
        return argumentBuilder.build();
    }

    //todo raw
    @ParameterizedTest
    @MethodSource("testMatrix")
    void testConsumerAndProducer(ConsumerType consumerType, ProducerType producerType, boolean rawData) throws InterruptedException, ExecutionException {
        //write method depends on producerType
        Consumer<String> write = suffix ->  write(suffix, producerType);

        //read depends on consumer type
        switch (consumerType) {
            case NORMAL -> testConsumeNormal(write);
            case REALTIME -> testConsumeRealtime(write);
            case SAVEDSEARCH -> testConsumeSavedSearch(write);
        }
    }

    private void testConsumeNormal(Consumer<String> write) {
        write.accept("_normal");

        List<Map<String, String>> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(String.format(
                        "search index=%s sourcetype=%s | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.TEST_INDEX, SplunkResource.SOURCE_TYPE))
                .post("/splunk/normal")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {
                });

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Irma_normal", result.get(0).get("name"));
        Assertions.assertEquals("Earth\"", result.get(0).get("from"));
        Assertions.assertEquals("Leonard_normal", result.get(1).get("name"));
        Assertions.assertEquals("Earth 2.0\"", result.get(1).get("from"));
        Assertions.assertEquals("Sheldon_normal", result.get(2).get("name"));
        Assertions.assertEquals("Alpha Centauri\"", result.get(2).get("from"));
    }

    private void testConsumeRealtime(Consumer<String> write) throws InterruptedException, ExecutionException {

        RestAssured.given()
                .body(String.format(
                        "search index=%s sourcetype=%s | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.TEST_INDEX, SplunkResource.SOURCE_TYPE))
                .post("/splunk/startRealtimePolling");

        //wait some time to start polling
        TimeUnit.SECONDS.sleep(3);
        write.accept("_realtime1");
        TimeUnit.SECONDS.sleep(1);
        write.accept("realtime2");
        TimeUnit.SECONDS.sleep(1);
        write.accept("_realtime3");
        //wait some time to gather the pulls from splunk server
        TimeUnit.SECONDS.sleep(3);
        //there should be some data from realtime search in direct (concrete values depends on the speed of writing into index)
        //test is asserting that there are some
        RestAssured.get("/splunk/directRealtimePolling")
                .then()
                .statusCode(200)
                .body(containsString("_realtime"));
    }

   private void testConsumeSavedSearch(Consumer<String> write) {
        //create saved search
        RestAssured.given()
                .baseUri("http://localhost")
                .port(ConfigProvider.getConfig().getValue(SplunkResource.PARAM_REMOTE_PORT, Integer.class))
                .contentType(ContentType.JSON)
                .param("name", SplunkTestResource.SAVED_SEARCH_NAME)
                .param("disabled", "0")
                .param("description", "descritionText")
                .param("search",
                        "index=" + SplunkTestResource.TEST_INDEX + " sourcetype=" + SplunkResource.SOURCE_TYPE)
                .post("/services/saved/searches")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        write.accept("_savedSearch");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(SplunkTestResource.SAVED_SEARCH_NAME)
                .post("/splunk/savedSearch")
                .then()
                .statusCode(200)
                .body(containsString("Name: Sheldon_savedSearch"))
                .body(containsString("Name: Leonard_savedSearch"))
                .body(containsString("Name: Irma_savedSearch"));
    }

    private void write(String suffix, ProducerType producerType) {
        Consumer<Map> write =  data -> RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("index", SplunkTestResource.TEST_INDEX)
                .body(data)
                .post("/splunk/write/" + producerType.name())
                .then()
                .statusCode(201);

       write.accept(CollectionHelper.mapOf("entity", "Name: Sheldon" + suffix + " From: Alpha Centauri"));
       write.accept(CollectionHelper.mapOf("entity", "Name: Leonard" + suffix + " From: Earth 2.0"));
       write.accept(CollectionHelper.mapOf("entity", "Name: Irma" + suffix + " From: Earth"));
    }

//    private String expectedResult(Map<String, String> data) {
//        String expectedResult = data.entrySet().stream()
//                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
//                .collect(Collectors.joining(" "));
//        return expectedResult;

}
