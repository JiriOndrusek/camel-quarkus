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

package org.apache.camel.quarkus.component.kudu.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.component.kudu.it.kerby.KerbyServer;
import org.apache.camel.util.CollectionHelper;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.component.kudu.it.KuduInfrastructureTestHelper.DOCKER_HOST;
import static org.apache.camel.quarkus.component.kudu.it.KuduInfrastructureTestHelper.KUDU_TABLET_NETWORK_ALIAS;
import static org.apache.camel.quarkus.component.kudu.it.KuduRoute.KUDU_AUTHORITY_CONFIG_KEY;

public class KuduTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(KuduTestResource.class);
    private static final int KUDU_MASTER_RPC_PORT = 7051;
    private static final int KUDU_MASTER_HTTP_PORT = 8051;
    private static final int KUDU_TABLET_RPC_PORT = 7050;
    private static final int KUDU_TABLET_HTTP_PORT = 8050;
    private static final String KUDU_IMAGE = ConfigProvider.getConfig().getValue("kudu.container.image", String.class);
    private static final String KUDU_MASTER_NETWORK_ALIAS = "kudu-master";

    private GenericContainer<?> masterContainer;
    private GenericContainer<?> tabletContainer;

    private KerbyServer kdcServer;

    private File tmpDir;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        Network kuduNetwork = Network.newNetwork();

        try {
            //create tmp dir for kerberos server
            tmpDir = Files.createTempDirectory("camel-quarkus-kudu-test").toFile();

            Path path = Paths.get(tmpDir.getAbsolutePath()).resolve("krb5.conf");
            if (!path.toFile().exists()) {
                Files.createFile(path);
            }

            String content = "" +
                    "[libdefaults]\n" +
                    "    default_realm = EXAMPLE.COM\n" +
                    "    udp_preference_limit = 1\n" +
                    "\n" +
                    "[realms]\n" +
                    "    EXAMPLE.COM = {\n" +
                    "        kdc = localhost:60088\n" +
                    "        admin_server = localhost:60088\n" +
                    "    }\n" +
                    "\n" +
                    "[domain_realm]\n" +
                    "    .example.com = EXAMPLE.COM\n" +
                    "    example.com = EXAMPLE.COM";

            Files.write(path, content.getBytes());

            //start kerby
            kdcServer = new KerbyServer();
            kdcServer.startServer(tmpDir.getAbsolutePath());
            kdcServer.createPrincipal("kudu-master", "kudu/kudu-master.example.com@EXAMPLE.COM", "changeit");
            kdcServer.createPrincipal("kudu-tserver", "kukudu/kudu-tserver.example.com@EXAMPLE.COM", "changeit");

        } catch (IOException | KrbException e) {
            throw new RuntimeException(e);
        }
        // Setup the Kudu master server container
        masterContainer = new GenericContainer<>(KUDU_IMAGE)
                .withCommand("master")
                //                                .withEnv("MASTER_ARGS", "--unlock_unsafe_flags=true")
                .withCopyToContainer(MountableFile.forClasspathResource("kudu-master"),
                        "/home/kudu/kudu-master")
                .withExposedPorts(KUDU_MASTER_RPC_PORT, KUDU_MASTER_HTTP_PORT)
                .withEnv("MASTER_ARGS", "--unlock_unsafe_flags=true " +
                        "--rpc_authentication=required " +
                        "--keytab_file=/home/kudu/kudu-master" +
                        "--stderrthreshold=0")
                .withNetwork(kuduNetwork)
                .withNetworkAliases(KUDU_MASTER_NETWORK_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .waitingFor(Wait.forListeningPort());
        masterContainer.start();

        // Force host name and port, so that the tablet container is accessible from KuduResource, KuduTest and KuduIT.
        Consumer<CreateContainerCmd> consumer = cmd -> {
            Ports portBindings = new Ports();
            portBindings.bind(ExposedPort.tcp(KUDU_TABLET_RPC_PORT), Ports.Binding.bindPort(KUDU_TABLET_RPC_PORT));
            portBindings.bind(ExposedPort.tcp(KUDU_TABLET_HTTP_PORT), Ports.Binding.bindPort(KUDU_TABLET_HTTP_PORT));
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(portBindings)
                    .withNetworkMode(kuduNetwork.getId());
            cmd.withHostName(KUDU_TABLET_NETWORK_ALIAS).withHostConfig(hostConfig);
        };

        // Setup the Kudu tablet server container
        tabletContainer = new GenericContainer<>(KUDU_IMAGE)
                .withCommand("tserver")
                //                                .withEnv("TSERVER_ARGS", "--unlock_unsafe_flags=true")
                .withCopyToContainer(MountableFile.forClasspathResource("kudu-tserver"),
                        "/home/kudu/kudu-tserver")
                .withExposedPorts(KUDU_MASTER_RPC_PORT, KUDU_MASTER_HTTP_PORT)
                .withEnv("TSERVER_ARGS", "--unlock_unsafe_flags=true " +
                        "--rpc_authentication=required " +
                        "--keytab_file=/home/kudu/kudu-tserver" +
                        "--stderrthreshold=0")
                .withExposedPorts(KUDU_MASTER_RPC_PORT, KUDU_MASTER_HTTP_PORT)
                .withEnv("KUDU_MASTERS", KUDU_MASTER_NETWORK_ALIAS)
                .withExposedPorts(KUDU_TABLET_RPC_PORT, KUDU_TABLET_HTTP_PORT)
                .withNetwork(kuduNetwork)
                .withNetworkAliases(KUDU_TABLET_NETWORK_ALIAS)
                .withCreateContainerCmdModifier(consumer)
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .waitingFor(Wait.forListeningPort());
        tabletContainer.start();

        // Print interesting Kudu servers connectivity information
        final String masterRpcAuthority = masterContainer.getHost() + ":"
                + masterContainer.getMappedPort(KUDU_MASTER_RPC_PORT);

        LOG.info("Kudu master RPC accessible at " + masterRpcAuthority);
        final String masterHttpAuthority = masterContainer.getHost() + ":"
                + masterContainer.getMappedPort(KUDU_MASTER_HTTP_PORT);
        LOG.info("Kudu master HTTP accessible at " + masterHttpAuthority);
        final String tServerRpcAuthority = tabletContainer.getHost() + ":"
                + tabletContainer.getMappedPort(KUDU_TABLET_RPC_PORT);
        LOG.info("Kudu tablet server RPC accessible at " + tServerRpcAuthority);
        final String tServerHttpAuthority = tabletContainer.getHost() + ":"
                + tabletContainer.getMappedPort(KUDU_TABLET_HTTP_PORT);
        LOG.info("Kudu tablet server HTTP accessible at " + tServerHttpAuthority);

        return CollectionHelper.mapOf(
                KUDU_AUTHORITY_CONFIG_KEY, masterRpcAuthority,
                DOCKER_HOST, DockerClientFactory.instance().dockerHostIpAddress());
    }

    @Override
    public void stop() {
        try {
            if (masterContainer != null) {
                masterContainer.stop();
            }
            if (tabletContainer != null) {
                tabletContainer.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occurred while stopping the KuduTestResource", ex);
        }

        try {
            if (kdcServer != null) {
                kdcServer.stopServer();
            }

            if (tmpDir != null && tmpDir.exists()) {
                Files.walk(tmpDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (KrbException | IOException ex) {
            LOG.error(String.format("An issue occurred while stopping the KerbyServer. Tmp folder '%s' was not deleted.",
                    tmpDir.getAbsolutePath()), ex);
        }
    }
}
