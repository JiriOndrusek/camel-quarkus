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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.jboss.logging.Logger;

@Path("/debezium-postgres")
@ApplicationScoped
public class DebeziumPostgresResource {

    public static final String DB_NAME = "postgresDB";
    public static final String DB_USERNAME = "user";
    public static final String DB_PASSWORD = "changeit";
    public static final String PROPERTY_HOSTNAME = "quarkus.postgres.hostname";
    public static final String PROPERTY_STORE_FOLDER = "quarkus.debezium.store.folder";
    public static final String PROPERTY_PORT = "quarkus.postgres.port";

    private static final Logger LOG = Logger.getLogger(DebeziumPostgresResource.class);

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/getEvent")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getEvent() throws Exception {
        final Exchange message = consumerTemplate.receive("direct:event", 2000);
        return message.getIn().getBody(String.class);
    }

}
