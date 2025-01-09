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
package org.apache.camel.quarkus.component.ssh.it;

import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class SshTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshTestResource.class);

    private static final int SSH_PORT = 2222;
    private static final String SSH_IMAGE = ConfigProvider.getConfig().getValue("openssh-server.container.image",
            String.class);

    private GenericContainer container;
    protected SshServer sshd;
    protected int securedPort;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
        LOGGER.info("Starting SSH container");

        try {
            container = new GenericContainer(SSH_IMAGE)
                    .withExposedPorts(SSH_PORT)
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", "test")
                    .withEnv("USER_PASSWORD", "password")
                    .waitingFor(Wait.forListeningPort());

            //            container.withCopyFileToContainer(MountableFile.forHostPath("target/classes/hostkey.pem"),
            //                    "/ssl/hostkey.pem")
            //                    .withEnv("PASSWORD_ACCESS", "/ssl/hostkey.pem");

            container.start();

            LOGGER.info("Started SSH container to {}:{}", container.getHost(),
                    container.getMappedPort(SSH_PORT).toString());

            securedPort = AvailablePortFinder.getNextAvailable();

            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(securedPort);
            sshd.setKeyPairProvider(new FileKeyPairProvider(Paths.get(getHostKey())));
            sshd.setCommandFactory(new TestEchoCommandFactory());
            sshd.setPasswordAuthenticator((username, password, session) -> true);
            sshd.setPublickeyAuthenticator((username, key, session) -> {
                return true;
            });
            sshd.start();

            LOGGER.info("Started SSHD server to {}:{}", container.getHost(),
                    securedPort);

            return Map.of(
                    "quarkus.ssh.host", "localhost",
                    "quarkus.ssh.port", container.getMappedPort(SSH_PORT).toString(),
                    "quarkus.ssh.secured-port", securedPort + "",
                    "ssh.username", "test",
                    "ssh.password", "password");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //todo proper path (no target)
    protected String getHostKey() {
        return "target/certs/ssh.key";
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping SSH container and server");

        try {
            if (container != null) {
                container.stop();
            }
            if (sshd != null) {
                sshd.stop(true);
                Thread.sleep(50);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
