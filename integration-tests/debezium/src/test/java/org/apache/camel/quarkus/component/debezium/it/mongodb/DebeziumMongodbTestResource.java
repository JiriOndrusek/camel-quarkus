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

import org.apache.camel.quarkus.component.debezium.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.it.Type;
import org.jboss.logging.Logger;
import org.testcontainers.containers.MongoDBContainer;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<MongoDBContainer> {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static final String DB_USERNAME = "user";
    private static final String DB_PASSWORD = "changeit";

    private static int DB_PORT = 27017;

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    @Override
    protected MongoDBContainer createContainer() {
        return new MongoDBContainer().withEnv("MONGO_DATABASE_USERNAME", DB_USERNAME).withEnv("MONGO_DATABASE_PASSWORD",
                DB_PASSWORD);

    }

    @Override
    protected String getJdbcUrl() {
        final String jdbcUrl = container.getReplicaSetUrl();

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
