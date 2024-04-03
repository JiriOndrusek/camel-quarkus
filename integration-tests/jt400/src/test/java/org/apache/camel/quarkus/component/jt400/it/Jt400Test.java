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

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.MessageQueue;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.jt400.Jt400Constants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
public class Jt400Test {

    //    @Test
    public void testDataQueue() {
        RestAssured.given()
                .body("Leonard")
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Leonard"));

        RestAssured.post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello Leonard"));
    }

    //    @Test
    public void testDataQueueBinary() {
        RestAssured.given()
                .body("Fred")
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Fred"));

        RestAssured.given()
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello Fred"));
    }

    //    @Test
    public void testKeyedDataQueue() {
        String key = "key1";
        String key2 = "key2";

        RestAssured.given()
                .body("Sheldon")
                .queryParam("key", key)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Sheldon"));

        RestAssured.given()
                .body("Sheldon2")
                .queryParam("key", key2)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Sheldon2"));

        RestAssured.given()
                .body(key)
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello Sheldon"))
                .body(Jt400Constants.KEY, Matchers.equalTo(key));

        RestAssured.given()
                .body(key)
                .queryParam("searchType", "GT")
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello Sheldon2"))
                .body(Jt400Constants.KEY, Matchers.equalTo(key2));
    }

        @Test
    public void testMessageQueue() {
        RestAssured.given()
                .body("Irma")
                .post("/jt400/messageQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Irma"));

        RestAssured.post("/jt400/messageQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.is("Hello Irma"))
                //check of headers
                .body(Jt400Constants.SENDER_INFORMATION, Matchers.not(Matchers.empty()))
                .body(Jt400Constants.MESSAGE_FILE, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_SEVERITY, Matchers.is(0))
                .body(Jt400Constants.MESSAGE_ID, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_TYPE, Matchers.is(4))
                .body(Jt400Constants.MESSAGE, Matchers.is("QueuedMessage: Hello Irma"));
        //Jt400Constants.MESSAGE_DFT_RPY && Jt400Constants.MESSAGE_REPLYTO_KEY are used only for a special
        // type of message which can not be created by the camel component (*INQUIRY)
    }

    @Test
    public void testInquiryMessageQueue() {
        //
        //        create message (asking)
        //
        //         create message with sendingReply = ture && Jt400Constants.MESSAGE_REPLYTO_KEY = message #1

        //        producer "jt400://username:password@localhost/qsys.lib/qusrsys.lib/myq.msgq?sendingReply=true")

        //        to "jt400://username:password@localhost/qsys.lib/qusrsys.lib/myq.msgq"

        /*
            @Metadata(description = "*Consumer:* The key of the message that will be replied to (if the `sendingReply` parameter is set to `true`). "
                            +
                            "*Producer:* If set, and if the message body is not empty, a new message will not be sent to the provided message queue. "
                            +
                            "Instead, a response will be sent to the message identified by the given key. " +
                            "This is set automatically when reading from the message queue if the `sendingReply` parameter is set to `true`.",
              javaType = "byte[]")
        String MESSAGE_REPLYTO_KEY = "CamelJt400MessageReplyToKey";
         */

//        getMessageQueue();

        RestAssured.given()
                .body("Irma")
                .post("/jt400/messageQueueInquiry/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("reply to: Irma"));

//        RestAssured.post("/jt400/messageQueue/read")
//                .then()
//                .statusCode(200)
//                .body("result", Matchers.is("Hello Irma"))
//                //check of headers
//                .body(Jt400Constants.SENDER_INFORMATION, Matchers.not(Matchers.empty()))
//                .body(Jt400Constants.MESSAGE_FILE, Matchers.is(""))
//                .body(Jt400Constants.MESSAGE_SEVERITY, Matchers.is(0))
//                .body(Jt400Constants.MESSAGE_ID, Matchers.is(""))
//                .body(Jt400Constants.MESSAGE_TYPE, Matchers.is(4))
//                .body(Jt400Constants.MESSAGE, Matchers.is("QueuedMessage: Hello Irma"));
//        //Jt400Constants.MESSAGE_DFT_RPY && Jt400Constants.MESSAGE_REPLYTO_KEY are used only for a special
//        // type of message which can not be created by the camel component (*INQUIRY)
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

    private MessageQueue getMessageQueue() {
        String jt400Url = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
        String jt400Username = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
        String jt400Password = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);


        AS400 as400 = new AS400(jt400Url, jt400Username, jt400Password);
//        MessageQueue queue = new MessageQueue(as400, jt400Endpoint.getConfiguration().getObjectPath());

        return null;

    }

}
