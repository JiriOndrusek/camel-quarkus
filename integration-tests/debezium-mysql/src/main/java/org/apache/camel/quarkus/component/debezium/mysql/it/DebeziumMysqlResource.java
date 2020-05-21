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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;

@Path("/debezium-mysql")
@ApplicationScoped
public class DebeziumMysqlResource {

    public static final String DB_USERNAME = "root";
    public static final String DB_PASSWORD = "test";

    private static long TIMEOUT = 2000;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/receiveEmptyMessages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveEmptyMessages(@QueryParam("hostname") String hostname,
            @QueryParam("port") int port,
            @QueryParam("offsetStorageFileName") String offsetStorageFileName,
            @QueryParam("databaseHistoryFileFilename") String databaseHistoryFileFilename)
            throws Exception {

        int i = 0;
        Exchange exchange;
        while (i++ < 10) {
            exchange = receiveAsExange(hostname, port, offsetStorageFileName, databaseHistoryFileFilename);
            System.out.println(">>>>>>>>>>>>.. received " + i + "'empty' exchange " + exchange);
            //if exchange is null (timeout), al messages are received
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
    public String receive(@QueryParam("hostname") String hostname,
            @QueryParam("port") int port,
            @QueryParam("offsetStorageFileName") String offsetStorageFileName,
            @QueryParam("databaseHistoryFileFilename") String databaseHistoryFileFilename) {
        Exchange exchange = receiveAsExange(hostname, port, offsetStorageFileName, databaseHistoryFileFilename);
        if (exchange == null) {
            return null;
        }
        return exchange.getIn().getBody(String.class);
    }

    public Exchange receiveAsExange(String hostname, int port, String offsetStorageFileName,
            String databaseHistoryFileFilename) {
        Exchange ret = consumerTemplate.receive("debezium-mysql:localhost?"
                + "databaseHostname=" + hostname
                + "&databasePort=" + port
                + "&databaseUser=" + DB_USERNAME
                + "&databasePassword=" + DB_PASSWORD
                + "&databaseServerId=223344"
                + "&databaseHistoryFileFilename=" + databaseHistoryFileFilename
                + "&databaseServerName=qa"
                + "&offsetStorageFileName=" + offsetStorageFileName, TIMEOUT);

        return ret;
    }
}
