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

package org.apache.camel.quarkus.component.mongodb.it;

import java.sql.SQLException;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class MongoDbTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbTestResource.class);

    private static final int MONGODB_PORT = 27017;
    private static final String MONGO_IMAGE = "mongo:4.0";

    private GenericContainer container;

    private static MongoClient mongoClient;

    private static MongoDatabase db;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer(MONGO_IMAGE)
                    .withExposedPorts(MONGODB_PORT)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            setUpDb();

            return CollectionHelper.mapOf(
                    "quarkus.mongodb.hosts",
                    container.getContainerIpAddress() + ":" + container.getMappedPort(MONGODB_PORT).toString(),
                    "quarkus.mongodb." + MongoDbResource.NAMED_MONGO_CLIENT_NAME + ".hosts",
                    container.getContainerIpAddress() + ":" + container.getMappedPort(MONGODB_PORT).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void setUpDb() throws SQLException {
        final String mongoUrl = "mongodb://" + container.getContainerIpAddress() + ":"
                + container.getMappedPort(MONGODB_PORT).toString();

        if (mongoUrl != null) {
            mongoClient = MongoClients.create(mongoUrl);
        }
        org.junit.Assume.assumeTrue(mongoClient != null);

        db = mongoClient.getDatabase("test");
        db.createCollection(MongoDbRoute.COLLECTION_TAILING,
                new CreateCollectionOptions().capped(true).sizeInBytes(1000000000).maxDocuments(MongoDbTest.CAP_NUMBER));
        db.createCollection(MongoDbRoute.COLLECTION_PERSISTENT_TAILING,
                new CreateCollectionOptions().capped(true).sizeInBytes(1000000000).maxDocuments(MongoDbTest.CAP_NUMBER));
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
