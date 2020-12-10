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

package org.apache.camel.quarkus.component.debezium.it.mongodb;

import java.time.Duration;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.apache.camel.quarkus.component.debezium.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.it.Type;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<GenericContainer> {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static final String DB_USERNAME = "debezium";
    private static final String DB_PASSWORD = "dbz";
    private static int DB_PORT = 27017;

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    @Override
    protected GenericContainer createContainer() {
        return new GenericContainer("mongo")
                .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) cmd -> {
                    cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(DB_PORT), new ExposedPort(DB_PORT)));
                })
                .withCommand("--replSet", "my-mongo-set")
                .waitingFor(new HttpWaitStrategy()
                        .forPath("/minio/health/ready")
                        .forPort(DB_PORT)
                        .withStartupTimeout(Duration.ofSeconds(10)));

    }

    @Override
    protected void startContainer() {
        super.startContainer();

        try {
            //intialize mongodb replica set
            Container.ExecResult er = container.execInContainer(new String[] { "mongo", "--eval",
                    "rs.initiate( {\"_id\" : \"my-mongo-set\",\t\"members\" : [{\"_id\" : 0, \"host\" : \""
                            + container.getContainerInfo().getNetworkSettings().getIpAddress()
                            + ":27017\", \"priority\": 1}] })" });
            System.out.println("========= " + er.getExitCode());
            er = container.execInContainer(new String[] { "mongo", "--eval",
                    "rs.initiate()" });
            System.out.println("========= " + er.getExitCode());
            er = container.execInContainer(new String[] { "mongo", "--eval",
                    "db.getSiblingDB('admin').runCommand({ createRole: \"listDatabases\",\n" +
                            "    privileges: [\n" +
                            "        { resource: { cluster : true }, actions: [\"listDatabases\"]}\n" +
                            "    ],\n" +
                            "    roles: []\n" +
                            "})" });
            System.out.println("========= " + er.getExitCode());
            er = container.execInContainer(new String[] { "mongo", "--eval",
                    "db.getSiblingDB('admin').createUser({ user: \"debezium\", pwd:\"dbz\", roles: [  {role: \"userAdminAnyDatabase\", db: \"admin\" }, {role: \"listDatabases\", db: \"admin\" }, { role: \"dbAdminAnyDatabase\", db: \"admin\" },  { role: \"readWriteAnyDatabase\", db:\"admin\" },  { role: \"clusterAdmin\",  db: \"admin\" }]});" });
            System.out.println("========= " + er.getExitCode());
            er = container.execInContainer(new String[] { "mongo", "--eval",
                    "rs.status();sh.addShard(\"my-mongo-set/" + container.getContainerInfo().getNetworkSettings().getIpAddress()
                            + ":27017\")" });
            System.out.println("========= " + er.getExitCode());
            //initial insert, without it no events will be received (it won't be present on replica list dbs)
            er = container.execInContainer(new String[] { "mongo", "--eval",
                    "db.test.insert({\"name\":\"init\"})" });
            System.out.println("========= " + er.getExitCode());
        } catch (Exception e) {
            Assert.fail("Initialization failed" + e.getMessage());
        }
    }

    @Override
    protected String getJdbcUrl() {
        final String jdbcUrl = "mongodb://" + DB_USERNAME + ":" + DB_PASSWORD + "@localhost:" + DB_PORT;

        return jdbcUrl;
    }

    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }
}
