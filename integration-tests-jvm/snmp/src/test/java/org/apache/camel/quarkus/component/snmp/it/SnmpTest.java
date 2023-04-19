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
package org.apache.camel.quarkus.component.snmp.it;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(SnmpTestResource.class)
class SnmpTest {

    private String listeningAddress;


    private Deque<PDU> receivedPDUs;

    @ParameterizedTest
    @ValueSource(ints = {SnmpConstants.version1, SnmpConstants.version2c})
    public void testSendReceiveTrap(int version) throws Exception {

        RestAssured.given()
                .body("TEXT" + version)
                .queryParam("version", version)
                .post("/snmp/sendTrap")
                .then()
                .statusCode(200);

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String result = RestAssured.given()
                    .body("trap")
                    .post("/snmp/results")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            return result.contains("TEXT" + version);
        });
    }

    @Test
    public void sendPDUs() throws Exception {

        RestAssured.given()
                .body("TEXT")
                .post("/snmp/sendPDU")
                .then()
                .statusCode(200);

        Thread.sleep(5000);
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> !receivedPDUs.isEmpty());
        System.out.println(receivedPDUs);
//        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
//            String result = RestAssured.given()
//                    .body("poll")
//                    .post("/snmp/results")
//                    .then()
//                    .statusCode(200)
//                    .extract().body().asString();
//
//            return result.contains("TEXT");
//        });

    }

    public void setListeningAddress(String listeningAddress) {
        this.listeningAddress = listeningAddress;
    }

    public void setReceivedPDUs(Deque<PDU> receivedPDUs) {
        this.receivedPDUs = receivedPDUs;
    }
}
