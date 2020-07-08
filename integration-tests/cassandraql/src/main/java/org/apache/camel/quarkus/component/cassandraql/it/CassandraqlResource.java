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
package org.apache.camel.quarkus.component.cassandraql.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.jboss.logging.Logger;

@Path("/cassandraql")
@ApplicationScoped
public class CassandraqlResource {
    public static final String DB_URL_PARAMETER = CassandraqlResource.class.getSimpleName() + "_db_url";
    public static final String KEYSPACE = "test";
    private static final Logger LOG = Logger.getLogger(CassandraqlResource.class);

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/insertEmployee")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void insertEmployee(Employee object) {
        LOG.infof("Sending to Cassandra: {%s}", object);
        producerTemplate.toF(createUrl("INSERT INTO employee(id, name, address) VALUES (?, ?, ?)"))
                .withBody(object.getValue())
                .request();
    }

    @Path("/getEmployee")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getEmployee(String id) throws Exception {
        final Exchange exchange = consumerTemplate.receive(createUrl(String.format("SELECT * FROM employee WHERE id = %s", id)));
        LOG.infof("Received from Cassandra: %s", exchange == null ? null : exchange.getIn().getBody());
        return exchange.getIn().getBody().toString();
    }

    @Path("/getAllEmployees")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllEmployees() throws Exception {
        final Exchange exchange = consumerTemplate.receive(createUrl("SELECT * FROM employee"));
        LOG.infof("Received from Cassandra: %s", exchange == null ? null : exchange.getIn().getBody());
        return exchange.getIn().getBody().toString();
    }

    @Path("/updateEmployee")
    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean updateEmployee(Employee employee) throws Exception {
        final Exchange exchange = consumerTemplate.receive(createUrl(String.format("UPDATE employee SET name = '%s', address = '%s' WHERE id = %s", employee.getName(), employee.getAddress(), employee.getId())));
        LOG.infof("Updated Employee with id :" + employee.getId());
        return exchange != null;
    }

    @Path("/deleteEmployee")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void updateEmployee(String id) throws Exception {
        consumerTemplate.receive(createUrl(String.format("DELETE FROM employee WHERE id = %s", id)));
        LOG.infof("Deleted Employee with id: " + id);
    }

    private String createUrl(String cql) {
        String url = System.getProperty(DB_URL_PARAMETER);
        return String.format("cql://%s/%s?cql=%s", url, KEYSPACE, cql);
    }
}
