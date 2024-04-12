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
package org.apache.camel.quarkus.component.jt400.it;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.jt400.Jt400Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
@QuarkusTestResource(Jt400TestResource.class)
public class Jt400Test {

    private final int MSG_LENGTH = 20;
    //tests may be executed in parallel, therefore the timeout is a little bigger in case the test has to wait for another one
    private final int WAIT_IN_SECONDS = 20;

    Jt400ClientHelper clientHelper;

    @Test
    public void testDataQueue() throws Exception {
        String msg = RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);

        //wait until queue is empty (mighy be used by the test running in parallel)
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> getClientHelper().peekLifoQueueEntry(),
                Matchers.nullValue());

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        //check the value
        DataQueueEntry dataQueueEntry = getClientHelper().peekLifoQueueEntry();
        //register to delete
        getClientHelper().clearLifoDataQueue(dataQueueEntry);

        RestAssured.post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    @Test
    public void testDataQueueBinary() throws Exception {
        String msg = RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);

        //wait until queue is empty (mighy be used by the test running in parallel)
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> getClientHelper().peekLifoQueueEntry(),
                Matchers.nullValue());

        RestAssured.given()
                .body(msg)
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        Map<String, DataQueueEntry> entries = new HashMap<>();
        //peek the value
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> {
                    DataQueueEntry dataQueueEntry = getClientHelper().peekLifoQueueEntry();
                    String entryMessage = new String(dataQueueEntry.getData(), StandardCharsets.UTF_8);
                    entries.put(entryMessage, dataQueueEntry);
                    return entryMessage;
                },
                Matchers.is("Hello " + msg));
        //register to delete
        getClientHelper().clearLifoDataQueue(entries.get("Hello " + msg));

        RestAssured.given()
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    @Test
    public void testKeyedDataQueue() {
        String msg1 = RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String msg2 = RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);

        getClientHelper()
                .addKeyedDataQueueKey1ToDelete(RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT));
        getClientHelper()
                .addKeyedDataQueueKey2ToDelete(RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT));

        RestAssured.given()
                .body(msg1)
                .queryParam("key", getClientHelper().getKey1ForKeyedDataQueue())
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg1));

        RestAssured.given()
                .body(msg2)
                .queryParam("key", getClientHelper().getKey2ForKeyedDataQueue())
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg2));

        RestAssured.given()
                .body(getClientHelper().getKey1ForKeyedDataQueue())
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg1))
                .body(Jt400Constants.KEY, Matchers.equalTo(getClientHelper().getKey1ForKeyedDataQueue()));

        RestAssured.given()
                .body(getClientHelper().getKey1ForKeyedDataQueue())
                .queryParam("searchType", "NE")
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.not(Matchers.equalTo("Hello " + msg1)))
                .body(Jt400Constants.KEY, Matchers.equalTo(getClientHelper().getKey2ForKeyedDataQueue()));
    }

    @Test
    public void testMessageQueue() throws Exception {

        //write
        String msg = RandomStringUtils.randomAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);

        Assertions.assertNull(getClientHelper().getQueueMessage("Hello " + msg));

        RestAssured.given()
                .body(msg)
                .post("/jt400/messageQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        QueuedMessage queueMessage = getClientHelper().getQueueMessage("Hello " + msg);
        Assertions.assertNotNull(queueMessage);
        //register to delete
        getClientHelper().addQueueMessageKeyToDelete(queueMessage.getKey());

        //read (the read message might be different in case the test runs in parallel

        msg = RestAssured.post("/jt400/messageQueue/read")
                .then()
                .statusCode(200)
                //check of headers
                .body(Jt400Constants.SENDER_INFORMATION, Matchers.not(Matchers.empty()))
                .body(Jt400Constants.MESSAGE_FILE, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_SEVERITY, Matchers.is(0))
                .body(Jt400Constants.MESSAGE_ID, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_TYPE, Matchers.is(4))
                .body(Jt400Constants.MESSAGE, Matchers.startsWith("QueuedMessage: Hello "))
                .extract().path("result").toString();
        //Jt400Constants.MESSAGE_DFT_RPY && Jt400Constants.MESSAGE_REPLYTO_KEY are used only for a special
        // type of message which can not be created by the camel component (*INQUIRY)

        Assertions.assertNotNull(getClientHelper().getQueueMessage(msg));

    }

    @Test
    public void testInquiryMessageQueue() throws Exception {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        //sending a message using the same client as component
        RestAssured.given()
                .body(msg)
                .post("/jt400/client/inquiryMessage/write")
                .then()
                .statusCode(200);

        //register deletion of the messae in case some following task fails
        QueuedMessage queueMessage = getClientHelper().getReplyToQueueMessage(msg);
        Assertions.assertNotNull(queueMessage);

        //start route before sending message
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/route/inquiryRoute/start")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(Boolean.TRUE.toString()));

        //await to be processed
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/inquiryMessageProcessed")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(String.valueOf(Boolean.TRUE)));

        //stop route
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/route/inquiryRoute/stop")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(Boolean.TRUE.toString()));

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).until(
                () -> {
                    QueuedMessage queuedMessage = getClientHelper().getReplyToQueueMessage("reply to: " + msg);
                    if (queuedMessage != null) {
                        //register to delete
                        getClientHelper().addReplyToMessageKeyToDelete(queueMessage.getKey());
                    }
                    return queuedMessage;
                },
                Matchers.notNullValue());

    }

    @Test
    public void testProgramCall() {
        RestAssured.given()
                .body("test")
                .post("/jt400/programCall")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello camel"));
    }

    public Jt400ClientHelper getClientHelper() {
        return clientHelper;
    }

    public void setClientHelper(Jt400ClientHelper clientHelper) {
        this.clientHelper = clientHelper;
    }
}
