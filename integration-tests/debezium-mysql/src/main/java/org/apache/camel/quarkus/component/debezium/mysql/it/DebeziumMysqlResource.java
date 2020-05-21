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
package org.apache.camel.quarkus.component.debezium.mysql.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;

@Path("/debezium-mysql")
@ApplicationScoped
public class DebeziumMysqlResource {

    public static final String PROPERTY_HOSTNAME = DebeziumMysqlResource.class.getName() + "_hostname";
    public static final String PROPERTY_PORT = DebeziumMysqlResource.class.getName() + "_port";
    public static final String PROPERTY_OFFSET_STORE_FILEPORT = DebeziumMysqlResource.class.getName()
            + "_offsetStorageFileName";
    public static final String PROPERTY_DB_HISTORY_FILE = DebeziumMysqlResource.class.getName()
            + "_databaseHistoryFileFilename";

    public static final String DB_USERNAME = "user";
    //debezium needs more privileges, therefore it will use root user
    public static final String DB_ROOT_USERNAME = "root";
    public static final String DB_PASSWORD = "test";

    private static long TIMEOUT = 2000;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/receiveEmptyMessages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveEmptyMessages() {

        int i = 0;
        Exchange exchange;
        while (i++ < 10) {
            exchange = receiveAsExange();
            //if exchange is null (timeout), all empty messages are received
            if (exchange == null) {
                return null;
            }
            //if exchange contains data, return value
            String value = exchange.getIn().getBody(String.class);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        Exchange exchange = receiveAsExange();
        if (exchange == null) {
            return null;
        }
        return exchange.getIn().getBody(String.class);
    }

    public Exchange receiveAsExange() {
        Exchange ret = consumerTemplate.receive("debezium-mysql:localhost?"
                + "databaseHostname=" + System.getProperty(DebeziumMysqlResource.PROPERTY_HOSTNAME)
                + "&databasePort=" + System.getProperty(DebeziumMysqlResource.PROPERTY_PORT)
                + "&databaseUser=" + DB_ROOT_USERNAME
                + "&databasePassword=" + DB_PASSWORD
                + "&databaseServerId=223344"
                + "&databaseHistoryFileFilename=" + System.getProperty(DebeziumMysqlResource.PROPERTY_DB_HISTORY_FILE)
                + "&databaseServerName=qa"
                + "&offsetStorageFileName=" + System.getProperty(DebeziumMysqlResource.PROPERTY_OFFSET_STORE_FILEPORT),
                TIMEOUT);

        return ret;
    }
}
