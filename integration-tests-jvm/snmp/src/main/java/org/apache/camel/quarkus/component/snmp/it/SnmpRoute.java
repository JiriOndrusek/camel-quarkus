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

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.eclipse.microprofile.config.ConfigProvider;
import org.snmp4j.mp.SnmpConstants;

@ApplicationScoped
public class SnmpRoute extends RouteBuilder {

    private String host = "0.0.0.0";
    private String port = "161"; //0 udp fails withdifferent reason
    private String oids = "1.3.6.1.2.1.1.1.0";

    @Inject
    @Named("snmpTrapResults")
    Map<String, Deque<SnmpMessage>> snmpResults;

    @Override
    public void configure() {
        //todo use annotation
        String listeningAddress = ConfigProvider.getConfig().getValue("snmpListenAddress", String.class);
        //PDFU producer
        from("direct:producePDU")
                .to("snmp://" + listeningAddress + "?retries=1&protocol=udp&oids=" + oids);

                //trap consumer
                from("direct:snmpTrap" + SnmpConstants.version1)
                        .log(LoggingLevel.INFO, "Sending Trap pdu ${body}")
                        .to("snmp:127.0.0.1:1662?protocol=udp&type=TRAP&snmpVersion=0");

                from("direct:snmpTrap" + SnmpConstants.version2c)
                        .log(LoggingLevel.INFO, "Sending Trap pdu ${body}")
                        .to("snmp:127.0.0.1:1662?protocol=udp&type=TRAP&snmpVersion=0");

                from("snmp:0.0.0.0:1662?protocol=udp&type=TRAP&snmpVersion=0")
                        .process(e -> snmpResults.get("trap").add(e.getIn().getBody(SnmpMessage.class)));

//                //todo try 0.0.0.0
//                        from(String.format("snmp:%s?protocol=udp&type=POLL", host, port, oids))
//                                .process(e -> snmpResults.get("poll").add(e.getIn().getBody(SnmpMessage.class)));
    }

    static class Producers {
        @jakarta.enterprise.inject.Produces
        @Singleton
        @Named("snmpTrapResults")
        Map<String, Deque<SnmpMessage>> snmpResults() {
            Map<String, Deque<SnmpMessage>> map = new ConcurrentHashMap<>();
            map.put("trap", new ConcurrentLinkedDeque());
            map.put("poll", new ConcurrentLinkedDeque());
            return map;
        }
    }
}
