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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

public class CyberarkVaultTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberarkVaultTestResource.class);
    private static final int SERVICEBUS_INNER_PORT = 5672;
    private Map<String, String> initArgs = new LinkedHashMap<>();
    private ComposeContainer container;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

    @Override
    public Map<String, String> start() {
        //        final SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();
        //todo use cyberark names
        final boolean realCredentialsProvided = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING") != null
                && System.getenv("AZURE_SERVICEBUS_QUEUE_NAME") != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();
            try {
                //copy docker-compose to tmp location
                File dockerComposeFile, configFile;
                //create tmp folder in target
                Path targetDir = Paths.get("target");
                Path tempDir = Files.createTempDirectory(targetDir, "docker-compose-");
                try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("docker-compose.yaml");) {
                    dockerComposeFile = File.createTempFile("cyberark-docker-compose-", ".yaml", tempDir.toFile());
                    Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                FileUtils.copyDirectory(new File(getClass().getResource("/conf").getFile()), tempDir.resolve("conf").toFile());

                container = new ComposeContainer(dockerComposeFile)
                        //                        .withEnv("ACCEPT_EULA", "Y")
                        //                        .withEnv("SERVICEBUS_EMULATOR_IMAGE",
                        //                                config.getValue("servicebus-emulator.container.image", String.class))
                        //                        .withEnv("SQL_EDGE_IMAGE", config.getValue("azure-sql-edge.container.image", String.class))
                        //                        .withEnv("CONFIG_FILE", configFile.getAbsolutePath())
                        //                        .withEnv("MSSQL_SA_PASSWORD", "12345678923456y!43")
                        //                        .withExposedService("emulator", SERVICEBUS_INNER_PORT)
                        .withLocalCompose(true)
                        .withLogConsumer("conjur", new Slf4jLogConsumer(LOGGER))
                        .waitingFor("conjur", Wait.forLogMessage(".*Listening on http://0.0.0.0:80.*", 1));

                container.start();

                Container.ExecResult er = container.getContainerByServiceName("conjurr").get()
                        .execInContainer("conjurctl", "account", "create", "myConjurAccount");

                System.out.println("result: " + er.getExitCode());
                System.out.println(er.getStdout());
                System.out.println("------------");
                System.out.println(er.getStderr());

                er = container.getContainerByServiceName("client").get()
                        .execInContainer("conjur", "init", "-i", "-u", "http://localhost", "-a", "myConjurAccount");

                System.out.println("result: " + er.getExitCode());
                System.out.println(er.getStdout());
                System.out.println("------------");
                System.out.println(er.getStderr());

                //                String connectionString = "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
                //                        .formatted(container.getServiceHost("emulator", SERVICEBUS_INNER_PORT),
                //                                container.getServicePort("emulator", SERVICEBUS_INNER_PORT));
                //                result.put("azure.servicebus.connection.string", connectionString);
                //                result.put("azure.servicebus.queue.name", "queue.1");
                //                result.put("azure.servicebus.topic.name", "topic.1");
                //                result.put("azure.servicebus.topic.subscription.name", "subscription.1");
                //
                //                //todo create policy
                //                ConjurClient conjurClient;
                //
                //                String url = "http://localhost:8080/";
                //                String account = "myConjurAccount";
                ////                String authToken = this.configuration.getAuthToken();
                //                String apiKey = this.configuration.getApiKey();
                //                String username = this.configuration.getUsername();
                //                String password = this.configuration.getPassword() ;
                ////                this.conjurClient = ConjurClientFactory.createWithApiKey(url, account, username, apiKey);
                //                conjurClient = new ConjurClientImpl(url, account, username, (String)null, apiKey, (String)null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

            if (container != null) {
                container.stop();
            }

        } catch (Exception e) {
            // ignored
        }
    }
}
