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
package org.apache.camel.quarkus.component.debezium.postgres.it;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.builder.RouteBuilder;

public class DebeziumPostgresRouteBuilder extends RouteBuilder {

    static final AtomicInteger EVENT_COUNTER = new AtomicInteger(0);

    @Override
    public void configure() throws IOException {
        System.out.println("++++++++++++++++++++++++ configuring route");

        from("debezium-postgres:localhost?"
                + "databaseHostname={{" + DebeziumPostgresResource.PROPERTY_HOSTNAME + "}}"
                + "&databasePort={{" + DebeziumPostgresResource.PROPERTY_PORT + "}}"
                + "&databaseUser=" + DebeziumPostgresResource.DB_USERNAME
                + "&databasePassword=" + DebeziumPostgresResource.DB_PASSWORD
                + "&databaseDbname=" + DebeziumPostgresResource.DB_NAME
                + "&databaseServerName=qa"
                + "&offsetStorageFileName={{" + DebeziumPostgresResource.PROPERTY_STORE_FOLDER + "}}" + File.pathSeparator
                + "store"
                //todo change default value in native extension
                + "&offsetStorage=org.apache.camel.quarkus.component.debezium.postgres.graal.storage.NativeFileOffsetBackingStore")
                        .log(">>> direct event created <<<<<")
                        .to("direct:event");
    }
}
