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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        write(Collections.singletonMap("name", "Sheldon"), "submit", SplunkTestResource.INDEX);
    }

    @Test
    public void testStream() {
        write(Collections.singletonMap("name", "Irma"), "stream", SplunkTestResource.INDEX);
    }

    @Test
    @Disabled //needs version, which is obtained in Service.login which is not called on free splunk server
    public void testTcp() {
        write(Collections.singletonMap("name", "Leonard"), "tcp", SplunkTestResource.INDEX);
    }

    @Test
    public void testSearchNormal() {
        write();

        List<Map<String, String>> result = read("normal");

        assertResult(result);
    }

    @Test
    public void testSearchRealtime() throws InterruptedException, ExecutionException {
        //realtime search return all data, which are not inserted into splunk index,
        //therefore it is not possible to simulate event which is already sent to splunk but not written into index
        //this call just simulates execution of logic to search for such an event on splunk, in case it can cause problems
        //in native mode

        Future<List<Map<String, String>>> resultRead = Executors.newSingleThreadExecutor().submit(() -> read("realtime"));
        read("realtime");
        write("_r1");
        write("_r2");
        write("_r3");
        write("_r4");
        write("_r5");
        //wait some time for the pulls from splunk server
        TimeUnit.SECONDS.sleep(5);
        //there should be some data in realtime search
        List result = resultRead.get();

        //but there is no assert, because data from search differs almost randomly
    }

    @Test
    @Disabled // todo
    public void testSavedSearch() {
    }

    private void assertResult(List<Map<String, String>> result) {
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Irma", result.get(0).get("name"));
        Assert.assertEquals("Earth\"", result.get(0).get("from"));
        Assert.assertEquals("Leonard", result.get(1).get("name"));
        Assert.assertEquals("Earth 2.0\"", result.get(1).get("from"));
        Assert.assertEquals("Sheldon", result.get(2).get("name"));
        Assert.assertEquals("Alpha Centauri\"", result.get(2).get("from"));
    }

    private List<Map<String, String>> read(String endpoint) {
        List<Map<String, String>> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(String.format(
                        "search index=%s sourcetype=%s | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                        SplunkTestResource.INDEX, SplunkResource.SOURCE_TYPE))
                .post("/splunk/" + endpoint)
                .then()
                .statusCode(200)
                .extract().as(List.class);
        return result;
    }

    private void write() {
        write("");
    }

    private void write(String suffix) {
        write(CollectionHelper.mapOf("entity", "Name: Sheldon" + suffix + " From: Alpha Centauri"), "submit",
                SplunkTestResource.INDEX);
        write(CollectionHelper.mapOf("entity", "Name: Leonard" + suffix + " From: Earth 2.0"), "submit",
                SplunkTestResource.INDEX);
        write(CollectionHelper.mapOf("entity", "Name: Irma" + suffix + " From: Earth"), "submit", SplunkTestResource.INDEX);
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
