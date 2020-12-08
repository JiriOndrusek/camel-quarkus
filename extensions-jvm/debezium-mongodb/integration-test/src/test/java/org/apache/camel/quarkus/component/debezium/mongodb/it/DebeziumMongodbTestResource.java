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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.testcontainers.containers.GenericContainer;

public class DebeziumMongodbTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static final String DB_USERNAME = "debezium";
    private static final String DB_PASSWORD = "dbz";
    private static int DB_PORT = 27017;

    private GenericContainer mongo;

    private static Path storeFile;

    @Override
    public Map<String, String> start() {

        mongo = new GenericContainer("mongo")
                .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) cmd -> {
                    cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(27017), new ExposedPort(27017)));
                })
                .withCommand("--replSet", "my-mongo-set");

        mongo.start();

        try {
            storeFile = Files.createTempFile(DebeziumMongodbTest.class.getSimpleName() + "-store-", "");

            //todo simple workaround to wait for container to start
            Thread.sleep(5000);

            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "rs.initiate( {\"_id\" : \"my-mongo-set\",\t\"members\" : [{\"_id\" : 0, \"host\" : \""
                            + mongo.getContainerInfo().getNetworkSettings().getIpAddress()
                            + ":27017\", \"priority\": 1}] })" });
            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "rs.initiate()" });
            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "db.getSiblingDB('admin').runCommand({ createRole: \"listDatabases\",\n" +
                            "    privileges: [\n" +
                            "        { resource: { cluster : true }, actions: [\"listDatabases\"]}\n" +
                            "    ],\n" +
                            "    roles: []\n" +
                            "})" });
            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "db.getSiblingDB('admin').createUser({ user: \"debezium\", pwd:\"dbz\", roles: [  {role: \"userAdminAnyDatabase\", db: \"admin\" }, {role: \"listDatabases\", db: \"admin\" }, { role: \"dbAdminAnyDatabase\", db: \"admin\" },  { role: \"readWriteAnyDatabase\", db:\"admin\" },  { role: \"clusterAdmin\",  db: \"admin\" }]});" });
            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "rs.status();sh.addShard(\"my-mongo-set/localhost:27017\")" });
            //initial insert, without it no events will be received (it won't be present on replica list dbs)
            mongo.execInContainer(new String[] { "mongo", "--eval",
                    "db.test.insert({\"name\":\"init\"})" });

            return CollectionHelper.mapOf(
                    DebeziumMongodbResource.PARAM_HOST, mongo.getHost(),
                    DebeziumMongodbResource.PARAM_EXPOSED_PORT, "27017",
                    //                    type.getPropertyPort(), container.getMappedPort(getPort()) + "",
                    DebeziumMongodbResource.PARAM_USERNAME, DB_USERNAME,
                    DebeziumMongodbResource.PARAM_PASSWORD, DB_PASSWORD,
                    DebeziumMongodbResource.PARAM_FILE, storeFile.toString(),
                    DebeziumMongodbResource.PARAM_CLIENT_URL, getClientUrl());
        } catch (Exception e) {
            Assert.fail("Initialization failed" + e.getMessage());
        }
        return null;
    }

    protected String getClientUrl() {
        return "mongodb://" + DB_USERNAME + ":" + DB_PASSWORD + "@localhost:27017";
    }

    @Override
    public void stop() {
        try {
            if (mongo != null) {
                mongo.stop();
            }
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
