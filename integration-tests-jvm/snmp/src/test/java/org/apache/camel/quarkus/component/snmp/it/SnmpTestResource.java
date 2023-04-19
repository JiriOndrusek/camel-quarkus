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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.component.snmp.SnmpMessage;
import org.apache.camel.util.CollectionHelper;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SnmpTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String LISTEN_ADDRESS = "snmpListenAddress";

    Snmp snmpAgent;
    DefaultUdpTransportMapping dtlstmCR;
    Deque<PDU> receivedPDUs = new ConcurrentLinkedDeque<>();


    @Override
    public Map<String, String> start() {
        try {
            final CommandResponder commandResponder = new CommandResponder() {
                final boolean[] messageReceived = {false};

                @Override
                public void processPdu(CommandResponderEvent event) {
                    receivedPDUs.add(event.getPDU());
                    event.setProcessed(true);
                }

            };

            dtlstmCR = new DefaultUdpTransportMapping(new UdpAddress("127.0.0.1/0"));
            snmpAgent = new Snmp(dtlstmCR);
            snmpAgent.addCommandResponder(commandResponder);
            snmpAgent.listen();

            return CollectionHelper.mapOf(LISTEN_ADDRESS, dtlstmCR.getListenAddress().toString().replaceFirst("/", ":"));

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (snmpAgent != null) {
            try {
                snmpAgent.close();
            } catch (IOException e) {
                //nothing
            }
        }
    }

    @Override
     public void inject(Object testInstance) {
        ((SnmpTest)testInstance).setListeningAddress(dtlstmCR.getListenAddress().toString());
        ((SnmpTest)testInstance).setReceivedPDUs(receivedPDUs);
    }
}
