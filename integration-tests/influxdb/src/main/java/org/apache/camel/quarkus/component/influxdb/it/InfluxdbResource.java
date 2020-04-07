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
package org.apache.camel.quarkus.component.influxdb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;
import org.jboss.logging.Logger;

@Path("/influxdb")
@ApplicationScoped
public class InfluxdbResource {

    private static final Logger LOG = Logger.getLogger(InfluxdbResource.class);

    public static final String INFLUXDB_CONNECTION_PROPERTY = "quarkus.influxdb.connection.url";
    public static final String INFLUXDB_VERSION = "1.7.10";

    private static final String INFLUXDB_CONNECTION = "http://{{" + INFLUXDB_CONNECTION_PROPERTY + "}}/";
    private static final String INFLUXDB_CONNECTION_NAME = "influxDb_connection";
    private static final String INFLUXDB_ENDPOINT_URL = "influxdb:" + INFLUXDB_CONNECTION_NAME;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/ping")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pingVersion() throws Exception {
        InfluxDB influxDB = InfluxDBFactory.connect(context.getPropertiesComponent().parseUri(INFLUXDB_CONNECTION));
        context.getRegistry().bind(INFLUXDB_CONNECTION_NAME, influxDB);

        Pong pong = producerTemplate.requestBody(INFLUXDB_ENDPOINT_URL + "?operation=ping", null, Pong.class);

        return pong.getVersion();
    }
}
