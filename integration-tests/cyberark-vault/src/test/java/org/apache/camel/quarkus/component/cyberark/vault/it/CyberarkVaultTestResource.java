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

package org.apache.camel.quarkus.component.cyberark.vault.it;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class CyberarkVaultTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberarkVaultTestResource.class);
    private static final int SERVICEBUS_INNER_PORT = 5672;
    private Map<String, String> initArgs = new LinkedHashMap<>();


    private PostgreSQLContainer database;
    private GenericContainer conjur;
    private GenericContainer client;
    private GenericContainer nginx;
    private GenericContainer jenkins;
    private GenericContainer secretless;
    private GenericContainer petStore;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

    @Override
    public Map<String, String> start() {
        Network conjurNetwork = Network.newNetwork();



//
//        //        final SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();
//        //todo use cyberark names
        final boolean realCredentialsProvided = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING") != null
                && System.getenv("AZURE_SERVICEBUS_QUEUE_NAME") != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();


            PostgreSQLContainer<?> database = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withNetwork(conjurNetwork)
                    .withNetworkAliases("postgres")
                    .withUsername("postgres")
                    .withPassword("SuperSecretPg");

            database.start();

            conjur = new GenericContainer<>(DockerImageName.parse("cyberark/conjur").toString());
            conjur.withNetwork(conjurNetwork)
                    .withEnv("DATABASE_URL", "postgres://postgres:SuperSecretPg@database/postgres")
                    .withEnv("CONJUR_DATA_KEY", "Eb/J6DQkr+/zBowIL8+5+kG8zAqUSVnN/VW3rySRwoM=")
                    .dependsOn(database)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withCommand("server")
                            .withExposedPorts(80);

            conjur.start();

            client = new GenericContainer<>(DockerImageName.parse("conjurinc/cli5"))
                    .withNetwork(conjurNetwork)
                    .dependsOn(conjur);

            nginx = new GenericContainer<>(DockerImageName.parse("nginx:1.13.6-alpine"))
                    .withNetwork(conjurNetwork)
                    .withExposedPorts(8443)
                    .dependsOn(conjur);

            jenkins = new GenericContainer<>(DockerImageName.parse("jenkins/jenkins:lts"))
                    .withNetwork(conjurNetwork)
                    .withExposedPorts(8080);

            secretless = new GenericContainer<>(DockerImageName.parse("cyberark/secretless-broker:latest"))
                    .withNetwork(conjurNetwork)
                    .dependsOn(conjur);

            petStore = new GenericContainer<>(DockerImageName.parse("cyberark/demo-app:latest"))
                    .withNetwork(conjurNetwork)
                    .withExposedPorts(8080)
                    .dependsOn(secretless);

            conjur.start();
            client.start();
            nginx.start();
            jenkins.start();
            secretless.start();
            petStore.start();

            //            try {
//                //copy docker-compose to tmp location
//                File dockerComposeFile, configFile;
//                try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("docker-compose.yaml");) {
//                    dockerComposeFile = File.createTempFile("cyberark-docker-compose-", ".yaml");
//                    Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                }
//
//                container = new ComposeContainer(dockerComposeFile)
//                        //                        .withEnv("ACCEPT_EULA", "Y")
//                        //                        .withEnv("SERVICEBUS_EMULATOR_IMAGE",
//                        //                                config.getValue("servicebus-emulator.container.image", String.class))
//                        //                        .withEnv("SQL_EDGE_IMAGE", config.getValue("azure-sql-edge.container.image", String.class))
//                        //                        .withEnv("CONFIG_FILE", configFile.getAbsolutePath())
//                        //                        .withEnv("MSSQL_SA_PASSWORD", "12345678923456y!43")
//                        //                        .withExposedService("emulator", SERVICEBUS_INNER_PORT)
//                        .withLocalCompose(true)
//                        .withLogConsumer("conjur_server", new Slf4jLogConsumer(LOGGER))
//                        .waitingFor("conjur_server", Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));
//
//                container.start();
//
//                //                String connectionString = "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
//                //                        .formatted(container.getServiceHost("emulator", SERVICEBUS_INNER_PORT),
//                //                                container.getServicePort("emulator", SERVICEBUS_INNER_PORT));
//                //                result.put("azure.servicebus.connection.string", connectionString);
//                //                result.put("azure.servicebus.queue.name", "queue.1");
//                //                result.put("azure.servicebus.topic.name", "topic.1");
//                //                result.put("azure.servicebus.topic.subscription.name", "subscription.1");
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AZURE_SERVICEBUS_CONNECTION_STRING and AZURE_SERVICEBUS_QUEUE_NAME env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
        }
        return result;
    }

    @Override
    public void stop() {
        try {

            if (database != null) {
                database.stop();
            }
            if (conjur != null) {
                conjur.stop();
            }
            if (client != null) {
                client.stop();
            }
            if (nginx != null) {
                nginx.stop();
            }
            if (jenkins != null) {
                jenkins.stop();
            }
            if (secretless != null) {
                secretless.stop();
            }
            if (petStore != null) {
                petStore.stop();
            }

        } catch (Exception e) {
            // ignored
        }
    }
}
