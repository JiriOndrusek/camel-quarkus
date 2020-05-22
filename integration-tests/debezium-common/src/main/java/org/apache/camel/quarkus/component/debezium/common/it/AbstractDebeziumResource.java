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
package org.apache.camel.quarkus.component.debezium.common.it;

import javax.inject.Inject;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.jboss.logging.Logger;

abstract class AbstractDebeziumResource {

    public static final String PROPERTY_HOSTNAME = AbstractDebeziumResource.class.getName() + "_hostname";
    public static final String PROPERTY_PORT = AbstractDebeziumResource.class.getName() + "_port";
    public static final String PROPERTY_OFFSET_STORE_FILEPORT = AbstractDebeziumResource.class.getName()
            + "_offsetStorageFileName";

    public static final String DB_USERNAME = "user";
    public static final String DB_PASSWORD = "test";

    private static final Logger LOG = Logger.getLogger(AbstractDebeziumResource.class);

    private static final long TIMEOUT = 2000;

    abstract String getEndpointComponent();

    String getEndpoinUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        String endpointUrl = getEndpointComponent() + ":localhost?"
                + "databaseHostname=" + hostname
                + "&databasePort=" + port
                + "&databaseUser=" + username
                + "&databasePassword=" + password
                + "&databaseServerName=" + databaseServerName
                + "&offsetStorageFileName=" + offsetStorageFileName;

        return endpointUrl;
    }

    @Inject
    ConsumerTemplate consumerTemplate;

    public String receive() {
        Exchange exchange = receiveAsExange();
        if (exchange == null) {
            return null;
        }
        return exchange.getIn().getBody(String.class);
    }

    public String receiveEmptyMessages() {

        int i = 0;
        Exchange exchange;
        while (i++ < 10) {
            exchange = receiveAsExange();
            //if exchange is null (timeout), all empty messages are received
            if (exchange == null) {
                return null;
            }
            System.out.println("Receiving empty message " + i + ": " + exchange);
            //if exchange contains data, return value
            String value = exchange.getIn().getBody(String.class);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    public Exchange receiveAsExange() {
        return consumerTemplate.receive(getEndpoinUrl(
                System.getProperty(AbstractDebeziumResource.PROPERTY_HOSTNAME),
                System.getProperty(AbstractDebeziumResource.PROPERTY_PORT),
                DB_USERNAME, DB_PASSWORD, "qa",
                System.getProperty(DebeziumMysqlResource.PROPERTY_OFFSET_STORE_FILEPORT)), TIMEOUT);
    }
}
