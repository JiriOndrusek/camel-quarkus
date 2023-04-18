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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.snmp4j.Snmp;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SnmpRoute extends RouteBuilder {

    @Inject
    @Named("snmpTrapResults")
    Deque<SnmpMessage> snmpTrapResults;

    @Override
    public void configure() {
        //trap consumer
        from("direct:snmpTrap")
                .log(LoggingLevel.INFO, "Sending Trap pdu ${body}")
                .to("snmp:127.0.0.1:1662?protocol=udp&type=TRAP&snmpVersion=0");

        from("snmp:0.0.0.0:1662?protocol=udp&type=TRAP&snmpVersion=0")
                .process(e -> snmpTrapResults.add(e.getIn().getBody(SnmpMessage.class)));
    }

    static class Producers {
        @jakarta.enterprise.inject.Produces
        @Singleton
        @Named("snmpTrapResults")
        Deque<SnmpMessage> snmpTrapResults() {
            return new ConcurrentLinkedDeque<>();
        }
    }
}
