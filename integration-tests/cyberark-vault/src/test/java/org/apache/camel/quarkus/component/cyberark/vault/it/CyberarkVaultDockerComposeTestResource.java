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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

public class CyberarkVaultDockerComposeTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberarkVaultDockerComposeTestResource.class);
    private static final int SERVICEBUS_INNER_PORT = 5672;
    private Map<String, String> initArgs = new LinkedHashMap<>();
    private ComposeContainer container;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

    @Override
    public Map<String, String> start() {
        final Map<String, String> result = new LinkedHashMap<>();
        try {
            //copy docker-compose to tmp location
            File dockerComposeFile, configFile;
            //create tmp folder in target
            Path targetDir = Paths.get("target");
            Path tempDir = Files.createTempDirectory(targetDir, "docker-compose-");
            try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("docker-compose.yml");) {
                dockerComposeFile = File.createTempFile("docker-compose-", ".yml", tempDir.toFile());
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
                    .waitingFor("conjur", Wait.forLogMessage(".* Listening on http.*", 1));

            container.start();

            Container.ExecResult er = container.getContainerByServiceName("conjur").get()
                    .execInContainer("conjurctl", "account", "create", "myConjurAccount");
            //admin key is the last word from stdout
            String adminKey = new LinkedList<>(Arrays.asList(er.getStdout().split("\\s"))).getLast();
            result.put("conjur.account", "myConjurAccount");
            System.out.println("result: " + er.getExitCode());
            System.out.println(er.getStdout());
            System.out.println("------------");
            System.out.println(er.getStderr());

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "init", "oss", "-u", "https://proxy", "-a", "myConjurAccount", "--self-signed");

            System.out.println("result: " + er.getExitCode());
            System.out.println(er.getStdout());
            System.out.println("------------");
            System.out.println(er.getStderr());

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "login", "-i", "admin", "-p", adminKey);

            System.out.println("result: " + er.getExitCode());
            System.out.println(er.getStdout());
            System.out.println("------------");
            System.out.println(er.getStderr());

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "policy", "load", "-b", "root", "-f", "policy/BotApp.yml");

            System.out.println("result: " + er.getExitCode());
            System.out.println(er.getStdout());
            System.out.println("------------");
            System.out.println(er.getStderr());

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // Read JSON from a file
                JsonNode jsonNode = objectMapper.readTree(er.getStdout());
                jsonNode.get("created_roles").get("myConjurAccount:host:BotApp/myDemoApp").get("id");

                result.put("conjur.read.username", "host/BotApp/myDemoApp");
                result.put("conjur.read.apiKey",
                        jsonNode.get("created_roles").get("myConjurAccount:host:BotApp/myDemoApp").get("api_key").textValue());
                result.put("conjur.write.username", "user/Dave@BotApp");
                result.put("conjur.write.apiKey",
                        jsonNode.get("created_roles").get("myConjurAccount:user:Dave@BotApp").get("api_key").textValue());
            } catch (IOException e) {
                e.printStackTrace();
            }

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "logout");

            System.out.println("result: " + er.getExitCode());
            System.out.println(er.getStdout());
            System.out.println("------------");
            System.out.println(er.getStderr());

        } catch (Exception e) {
            throw new RuntimeException(e);
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
