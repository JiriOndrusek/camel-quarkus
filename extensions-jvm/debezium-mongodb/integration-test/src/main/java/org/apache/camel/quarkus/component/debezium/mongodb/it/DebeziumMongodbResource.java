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
package org.apache.camel.quarkus.component.debezium.mongodb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/debezium-mongodb")
@ApplicationScoped
public class DebeziumMongodbResource {

    @ConfigProperty(name = "test.debezium.timeout", defaultValue = "10000")
    private long TIMEOUT;

    public static final String PARAM_DB_NAME = DebeziumMongodbResource.class.getSimpleName() + "_db";
    public static final String PARAM_CLIENT_URL = DebeziumMongodbResource.class.getSimpleName() + "_clientUrl";
    public static final String PARAM_FILE = DebeziumMongodbResource.class.getSimpleName() + "_file";
    public static final String PARAM_USERNAME = DebeziumMongodbResource.class.getSimpleName() + "_debezium";
    public static final String PARAM_PASSWORD = DebeziumMongodbResource.class.getSimpleName() + "_dbz";
    public static final String PARAM_EXPOSED_PORT = DebeziumMongodbResource.class.getSimpleName() + "_exposedPort";
    public static final String PARAM_HOST = DebeziumMongodbResource.class.getSimpleName() + "_host";

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        Record record = receiveAsRecord();
        //mssql return empty Strring instead of nulls, wich leads to different status code 200 vs 204
        if (record == null || ("d".equals(record.getOperation()) && "".equals(record.getValue()))) {
            return null;
        }
        return record.getValue();
    }

    @Path("/receiveOperation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveOperation() {
        Record record = receiveAsRecord();
        //mssql return empty Strring instead of nulls, wich leads to different status code 200 vs 204
        if (record == null || ("d".equals(record.getOperation()) && "".equals(record.getValue()))) {
            return null;
        }
        return record.getOperation();
    }

    private Exchange receiveAsExchange() {
        String endpoint = getEndpoinUrl();
        return consumerTemplate.receive(endpoint, TIMEOUT);
    }

    public Record receiveAsRecord() {
        Exchange exchange = receiveAsExchange();
        if (exchange == null) {
            return null;
        }
        return new Record(exchange.getIn().getHeader(DebeziumConstants.HEADER_OPERATION, String.class),
                exchange.getIn().getBody(String.class));
    }

    public String getEndpoinUrl() {
        return "debezium-mongodb:NAME_OF_ENDPOINT?"
                + "offsetStorageFileName=" + System.getProperty(PARAM_FILE)
                + "&mongodbUser=" + System.getProperty(PARAM_USERNAME)
                + "&mongodbPassword=" + System.getProperty(PARAM_PASSWORD)
                + "&mongodbName=" + System.getProperty(PARAM_DB_NAME)
                + "&mongodbHosts=" + System.getProperty(PARAM_HOST) + ":" + System.getProperty(PARAM_EXPOSED_PORT);
    }
}
