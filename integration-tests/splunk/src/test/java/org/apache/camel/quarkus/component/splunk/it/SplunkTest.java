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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.util.CollectionHelper;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(SplunkTestResource.class)
class SplunkTest {

    @Test
    public void testSubmit() {
        write(Collections.singletonMap("name", "Sheldon"), "submit", SplunkTestResource.SUBMIT_INDEX);
    }

    @Test
    public void testStream() {
        write(Collections.singletonMap("name", "Irma"), "stream", SplunkTestResource.STREAM_INDEX);
    }

    @Test
    @Disabled //needs to configure listening port (not 9997) in settings->data input -> tcp
    public void testTcp() {
        write(Collections.singletonMap("name", "Leonard"), "tcp", SplunkTestResource.SUBMIT_INDEX);
    }

    @Test
    public void testSearchNormal() {
        write(SplunkTestResource.NORMAL_SEARCH_INDEX);

        List<Map<String, String>> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(String.format(
                        "search index=%s sourcetype=%s | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.NORMAL_SEARCH_INDEX, SplunkResource.SOURCE_TYPE))
                .post("/splunk/normal")
                .then()
                .statusCode(200)
                .extract().as(List.class);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Irma", result.get(0).get("name"));
        Assert.assertEquals("Earth\"", result.get(0).get("from"));
        Assert.assertEquals("Leonard", result.get(1).get("name"));
        Assert.assertEquals("Earth 2.0\"", result.get(1).get("from"));
        Assert.assertEquals("Sheldon", result.get(2).get("name"));
        Assert.assertEquals("Alpha Centauri\"", result.get(2).get("from"));
    }

    @Test
    public void testSearchRealtime() throws InterruptedException, ExecutionException {

        RestAssured.given()
                .body(String.format(
                        "search index=%s sourcetype=%s | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.REALTIME_SEARCH_INDEX, SplunkResource.SOURCE_TYPE))
                .post("/splunk/startRealtimePolling");

        //some time to start polling
        TimeUnit.SECONDS.sleep(5);
        write("_r1", SplunkTestResource.REALTIME_SEARCH_INDEX);
        TimeUnit.SECONDS.sleep(1);
        write("_r2", SplunkTestResource.REALTIME_SEARCH_INDEX);
        TimeUnit.SECONDS.sleep(1);
        write("_r3", SplunkTestResource.REALTIME_SEARCH_INDEX);
        TimeUnit.SECONDS.sleep(1);
        write("_r4", SplunkTestResource.REALTIME_SEARCH_INDEX);
        TimeUnit.SECONDS.sleep(1);
        write("_r5", SplunkTestResource.REALTIME_SEARCH_INDEX);
        //wait some time to gather the pulls from splunk server
        TimeUnit.SECONDS.sleep(5);
        //there should be some data from realtime search in direct (concrete values depends on the speed of writing into index)
        //test is asserting that there are some
        RestAssured.get("/splunk/directRealtimePolling")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSavedSearch() throws InterruptedException {
        int defaultPort = RestAssured.port;
        String defaultUri = RestAssured.baseURI;

        try {
            RestAssured.port = Integer.parseInt(System.getProperty(SplunkResource.PARAM_REMOTE_PORT));
            RestAssured.baseURI = "http://localhost";

            //create saved search
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .param("name", SplunkTestResource.SAVED_SEARCH_NAME)
                    .param("disabled", "0")
                    .param("description", "descritionText")
                    .param("search",
                            "index=" + SplunkTestResource.SAVED_SEARCH_INDEX + " sourcetype=" + SplunkResource.SOURCE_TYPE)
                    .post("/services/saved/searches")
                    .then()
                    .statusCode(201);
        } finally {
            RestAssured.port = defaultPort;
            RestAssured.baseURI = defaultUri;
        }

        write("_s", SplunkTestResource.SAVED_SEARCH_INDEX);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(SplunkTestResource.SAVED_SEARCH_NAME)
                .post("/splunk/savedSearch")
                .then()
                .statusCode(200)
                .body(containsString("Name: Sheldon_s"))
                .body(containsString("Name: Leonard_s"))
                .body(containsString("Name: Irma_s"));
    }

    private void write(String index) {
        write("", index);
    }

    private void write(String suffix, String index) {
        write(CollectionHelper.mapOf("entity", "Name: Sheldon" + suffix + " From: Alpha Centauri"), "submit",
                index);
        write(CollectionHelper.mapOf("entity", "Name: Leonard" + suffix + " From: Earth 2.0"), "submit",
                index);
        write(CollectionHelper.mapOf("entity", "Name: Irma" + suffix + " From: Earth"), "submit", index);
    }

    private void write(Map<String, String> data, String endpoint, String index) {

        String expectedResult = expectedResult(data);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("index", index)
                .body(data)
                .post("/splunk/" + endpoint)
                .then()
                .statusCode(201)
                .body(containsString(expectedResult));
    }

    private String expectedResult(Map<String, String> data) {
        String expectedResult = data.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(" "));
        return expectedResult;
    }

}
