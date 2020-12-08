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

package org.apache.camel.quarkus.component.debezium.common.it.mongodb;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<GenericContainer> {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static final String DB_USERNAME = "debezium";
    private static final String DB_PASSWORD = "dbz";

    private static int DB_PORT = 27017;

    private GenericContainer mongo1, mongo2, initiator;

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    @Override
    protected GenericContainer createContainer() {
        //        https://github.com/aashreys/docker-mongo-auth
        //        return new GenericContainer(DockerImageName.parse("aashreys/mongo-auth:latest"))
        //                .withEnv("AUTH", "true")
        ////                .withEnv("MONGODB_APPLICATION_USER", "admin")
        ////                .withEnv("MONGODB_APPLICATION_PASS", "admin")
        ////                .withEnv("ENV MONGODB_ADMIN_USER", "admin")
        ////                .withEnv("ENV MONGODB_ADMIN_PASS", "admin")
        //                .withEnv("MONGODB_APPLICATION_USER", DB_USERNAME)
        //                .withEnv("MONGODB_APPLICATION_PASS", DB_PASSWORD)
        //                .withEnv("MONGODB_APPLICATION_DATABASE", "inventory")
        //                .withExposedPorts(DB_PORT);

        //        return new GenericContainer(DockerImageName.parse("aashreys/mongo-auth:latest"))
        //                .withEnv("AUTH", "true")
        //                .withEnv("MONGODB_APPLICATION_USER", DB_USERNAME)
        //                .withEnv("MONGODB_APPLICATION_PASS", DB_PASSWORD)
        //                .withEnv("MONGODB_APPLICATION_DATABASE", "test")
        //                .withExposedPorts(DB_PORT);

        //        Network network = Network.newNetwork();

        //        mongo1 = new GenericContainer("mongo")
        //                .withNetwork(network)
        //                .withNetworkAliases("mongo1")
        //                .withCommand("--replSet", "my-mongo-set");

        mongo1 = new MongoDBContainer(
                "mongo:4.2")/*.withNetwork(network)*//*.withCreateContainerCmdModifier(cmd -> cmd.withName("mongo1"))*/;
        //        mongo1 = new GenericContainer("mongo:4.2")./*withNetwork(network).*/withCommand("--repl-set"," my-mongo-set")/*.withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
        //            @Override
        //            public void accept(CreateContainerCmd createContainerCmd) {
        //                createContainerCmd.withName("mongo1");
        //            }
        //        })*/;
        //        mongo2 = new MongoDBContainer("mongo:4.2")/*.withNetwork(network)*/.withCreateContainerCmdModifier(cmd -> cmd.withName("mongo2"));
        //        mongo2 = new GenericContainer("mongo:4.2")/*.withNetwork(network)*//*.withCommand("--repl-set", "my-mongo-set")*//*.withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
        //            @Override
        //            public void accept(CreateContainerCmd createContainerCmd) {
        //                createContainerCmd.withName("mongo2");
        //            }
        //        })*/

        //        mongo2 = new GenericContainer("mongo")
        ////                .withNetwork(network)
        //                .withNetworkAliases("mongo2")
        //                .withCommand("--replSet my-mongo-set");
        //
        //        initiator = new GenericContainer("debezium/mongo-initiator")
        //                .withNetwork(network)
        //                .withNetworkAliases("mongo-init")
        //                .withEnv("REPLICASET", "docker-rs --link mongo1:mongo1 --link mongo2:mongo2");

        mongo1.start();

        try {
            Container.ExecResult /*er = mongo1.execInContainer(new String[] { "mongo", "--eval", "rs.initiate()" });
                                 System.out.println("+++++++++++++++" + er.getStdout());*/
            er = mongo1.execInContainer(new String[] { "mongo", "--eval",
                    "db.getSiblingDB('admin').createUser({ user: \"debezium\", pwd:\"dbz\", roles: [  {role: \"userAdminAnyDatabase\", db: \"admin\" }, { role: \"dbAdminAnyDatabase\", db: \"admin\" },  { role: \"readWriteAnyDatabase\", db:\"admin\" },  { role: \"clusterAdmin\",  db: \"admin\" }]});" });
            System.out.println("+++++++++++++++" + er.getStdout());
            er = mongo1
                    .execInContainer(new String[] { "mongo", "--eval", " db = (new Mongo('localhost:27017')).getDB('test')" });
            System.out.println("+++++++++++++++" + er.getStdout());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //        mongo2.start();
        //        initiator.start();

        return mongo1;

        //        return new GenericContainer(DockerImageName.parse("debmongodb:1.4"))
        //                .withEnv("AUTH", "true")
        //                .withEnv("MONGODB_APPLICATION_USER", DB_USERNAME)
        //                .withEnv("MONGODB_APPLICATION_PASS", DB_PASSWORD)
        //                .withEnv("MONGODB_APPLICATION_DATABASE", "inventory")
        //                .withExposedPorts(DB_PORT);

        //        return new GenericContainer(DockerImageName.parse("docker.io/debezium/example-mongodb:1.3"));
        //                .withEnv("AUTH", "true")
        //                .withEnv("MONGODB_APPLICATION_USER", DB_USERNAME)
        //                .withEnv("MONGODB_APPLICATION_PASS", DB_PASSWORD)
        //                .withEnv("MONGODB_APPLICATION_DATABASE", "test")
        //                .withExposedPorts(DB_PORT);

        //        return new GenericContainer(DockerImageName.parse("mongo:latest"))
        //                .withEnv("MONGO_INITDB_ROOT_USERNAME", DB_USERNAME)
        //                .withEnv("MONGO_INITDB_ROOT_PASSWORD", DB_PASSWORD)
        //                .withEnv("MONGO_INITDB_DATABASE", "test")
        //                .withExposedPorts(DB_PORT);
        //        return new MongoDBContainer("mongo:latest").withEnv("MONGO_DATABASE_USERNAME", DB_USERNAME).withEnv("MONGO_DATABASE_PASSWORD",
        //                DB_PASSWORD);

    }

    @Override
    protected String getJdbcUrl() {
        //        final String jdbcUrl = container.getMappedPort(DB_PORT) + "";
        //         String jdbcUrl = "mongodb://" + DB_USERNAME + ":" + DB_PASSWORD + "@localhost:30001";
        //        String jdbcUrl = "mongodb://" + DB_USERNAME + ":" + DB_PASSWORD + "@localhost:27017";
        String jdbcUrl = "mongodb://" + DB_USERNAME + ":" + DB_PASSWORD + "@localhost:" + container.getMappedPort(DB_PORT);
        //((MongoDBContainer)container).getReplicaSetUrl();
        //jdbcUrl = ((MongoDBContainer)mongo1).getReplicaSetUrl();
        return jdbcUrl;
    }

    //    @Override
    //    public Map<String, String> start() {
    //        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
    //        try {
    //            storeFile = Files.createTempFile(getClass().getSimpleName() + "-store-", "");
    //
    //            createContainer();
    //
    //            mongo1.start();
    //            mongo2.start();
    //            initiator.start();
    //
    ////            String out = mongo1.execInConMongoDBContainertainer("mongo", "use admin;").getStdout();
    ////            System.out.println("*******************************8");
    ////            System.out.println(out);
    ////            System.out.println("*******************************8");
    //
    //            Map<String, String> map = CollectionHelper.mapOf(
    //                        type.getPropertyHostname(), container.getContainerIpAddress(),
    //                    type.getPropertyPort(),  container.getMappedPort(getPort()) + "",
    //                    type.getPropertyUsername(), getUsername(),
    //                    type.getPropertyPassword(), getPassword(),
    //                    type.getPropertyOffsetFileName(), storeFile.toString(),
    //                    type.getPropertyJdbc(), getJdbcUrl());
    //
    //            return map;
    //
    //        } catch (Exception e) {
    //            LOGGER.error("Container does not start", e);
    //            throw new RuntimeException(e);
    //        }
    //    }

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

    @Override
    public void stop() {
        try {
            if (mongo1 != null) {
                mongo1.stop();
            }
            if (mongo2 != null) {
                mongo2.stop();
            }
            if (initiator != null) {
                initiator.stop();
            }
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
