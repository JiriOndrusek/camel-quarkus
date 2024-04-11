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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.jt400.Jt400Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
@QuarkusTestResource(Jt400TestResource.class)
public class Jt400Test {

    Jt400ClientHelper clientHelper;

    @Test
    public void testDataQueue() {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        RestAssured.post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    @Test
    public void testDataQueueBinary() {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        RestAssured.given()
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    //works in parallel, each test has random keys
    @Test
    public void testKeyedDataQueue() {
        String msg1 = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);
        String msg2 = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);

        getClientHelper().addKeyedDataQueueKey1ToDelete(RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT));
        getClientHelper().addKeyedDataQueueKey2ToDelete(RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT));

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
//
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

    //works in parallel
    @Test
    public void testMessageQueue() throws Exception {

        //write
        String msg = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);

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
    @Disabled //CPF2451 Message queue REPLYMSGQ is allocated to another job.
    public void testInquiryMessageQueue() {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);


        //start route before sending message
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/route/inquiryRoute/start")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(Boolean.TRUE.toString()));


        try {
            //sending a message using the same client as component
            RestAssured.given()
                    .body(msg)
                    .post("/jt400/client/inquiryMessage/write")
                    .then()
                    .statusCode(200);


            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(
                    () -> RestAssured.given()
                        .body(ConfigProvider.getConfig().getValue("cq.jt400.message-replyto-queue", String.class))
                        .post("/jt400/client/queuedMessage/read")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                    Matchers.is("reply to: " + msg));

        } finally {
            //stop route after the test sending message
            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
                    () -> RestAssured.get("/jt400/route/inquiryRoute/start")
                            .then()
                            .statusCode(200)
                            .extract().asString(),
                    Matchers.is(Boolean.TRUE.toString()));
        }
    }

    //works in parallel
    @Test
    public void testProgramCall() {
        RestAssured.given()
                .body("test")
                .post("/jt400/programCall")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello camel"));
    }

 /*   private static void clearQueue(String queue, BiFunctionWithException<AS400, String, Object> readFromQueue) {
        String jt400Url = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
        String jt400Username = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
        String jt400Password = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
        String jt400Library = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
        String jt400MessageQueue = ConfigProvider.getConfig().getValue(queue, String.class);

        String objectPath = String.format("/QSYS.LIB/%s.LIB/%s", jt400Library, jt400MessageQueue);

        AS400 as400 = new AS400(jt400Url, jt400Username, jt400Password);

        int i = 0;
        Object msg;
        //read messages until null is received
        do {
            try {
                msg = readFromQueue.apply(as400, objectPath);
            } catch (Exception e) {
                throw new IllegalStateException("Error when clearing queue " + jt400MessageQueue + "!", e);
            }
        } while (i++ < 10 && msg != null);

        as400.close();

        if (i == 10 && msg != null) {
            throw new IllegalStateException("There is a message present in a queue " + jt400MessageQueue + "!");
        }
    }

    private static AS400 getAs400()  {
        String jt400Url = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
        String jt400Username = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
        String jt400Password = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);

        return new AS400(jt400Url, jt400Username, jt400Password);
    }

    private static MessageQueue getMessageQueue() throws Exception {
        String jt400MessageQueue = ConfigProvider.getConfig().getValue("cq.jt400.message-queue", String.class);
        String jt400Library = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);

        String objectPath = String.format("/QSYS.LIB/%s.LIB/%s", jt400Library, jt400MessageQueue);

        return new MessageQueue(getAs400(), objectPath);
    }

   private static void clearMessageQueue() throws Exception {
        String jt400Url = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
        String jt400Username = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
        String jt400Password = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
        String jt400Library = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
        String jt400MessageQueue = ConfigProvider.getConfig().getValue("cq.jt400.message-queue", String.class);

        String objectPath = String.format("/QSYS.LIB/%s.LIB/%s", jt400Library, jt400MessageQueue);

        AS400 as400 = new AS400(jt400Url, jt400Username, jt400Password);

        try {
            MessageQueue queue = new MessageQueue(as400, objectPath);

            messageQueueKeys.forEach(key -> {
                try {
                    queue.remove(key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            //if keys is null, clear whole message queue
//            if (keys == null) {
//                queue.remove();
//            Enumeration<QueuedMessage> msgsEnumeration = queue.getMessages();
//            while(msgsEnumeration.hasMoreElements()) {
//                QueuedMessage msg = msgsEnumeration.nextElement();
//                queue.remove();
//            }
//            }
        } finally {
          as400.close();
        }
//            List<Messag> msgs = Collections.list()
//
//        int i = 0;
//        Object msg;
//        //read messages until null is received
//        do {
//            try {
//                msg = readFromQueue.apply(as400, objectPath);
//            } catch (Exception e) {
//                throw new IllegalStateException("Error when clearing queue " + jt400MessageQueue + "!", e);
//            }
//        } while (i++ < 10 && msg != null);
//
//        as400.close();
//
//        if (i == 10 && msg != null) {
//            throw new IllegalStateException("There is a message present in a queue " + jt400MessageQueue + "!");
//        }
    }

    @FunctionalInterface
    private interface BiFunctionWithException<T, U, R> {
        R apply(T t, U u) throws Exception;
    }
*/
    public Jt400ClientHelper getClientHelper() {
        return clientHelper;
    }

    public void setClientHelper(Jt400ClientHelper clientHelper) {
        this.clientHelper = clientHelper;
    }
}

