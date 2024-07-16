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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.testcontainers.images.builder.ImageFromDockerfile;
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

    private String kerbyDir;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        Network kuduNetwork = Network.newNetwork();

        try {
            //create tmp dir for kerberos server
            kerbyDir = getClass().getResource("/kerby").getFile();

            //start kerby
            kdcServer = new KerbyServer();
            kdcServer.startServer(kerbyDir);
            kdcServer.createAndExportPrincipals(KUDU_MASTER_NETWORK_ALIAS, "kudu/" + KUDU_MASTER_NETWORK_ALIAS + "@EXAMPLE.COM", "changeit"); //equivalent to hostname of container
            kdcServer.createAndExportPrincipals(KUDU_TABLET_NETWORK_ALIAS,  "kudu/" + KUDU_TABLET_NETWORK_ALIAS + "@EXAMPLE.COM", "changeit");
            kdcServer.createAndExportPrincipals("kudu-user",  "kudu/user@EXAMPLE.COM", "changeit");

            //replace "localhost" in krb5.conf with ip address
            Path path = Path.of(getClass().getResource("/kerby/krb5.conf").getFile());
            Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll("localhost", IpAddressHelper.getHost4Address());
            Files.write(path, content.getBytes(charset));

        } catch (IOException | KrbException e) {
            throw new RuntimeException(e);
        }

        masterContainer = new GenericContainer<>(new ImageFromDockerfile()
                .withDockerfile(Path.of(this.getClass().getResource("/kerby/Dockerfile").getFile())))
                .withCommand("master")
                .withExposedPorts(KUDU_MASTER_RPC_PORT, KUDU_MASTER_HTTP_PORT)
                .withEnv("MASTER_ARGS", "--unlock_unsafe_flags=true " +
                        "--rpc_authentication=required " +
                        "--keytab_file=/home/kudu/" + KUDU_MASTER_NETWORK_ALIAS + " " +
                        "--allow_world_readable_credentials=true " + //https://kudu.apache.org/docs/prior_release_notes.html
                        "--stderrthreshold=0 "
                )
                .withCopyToContainer(MountableFile.forClasspathResource("kerby/krb5.conf"),
                        "/etc/krb5.conf")
                .withNetwork(kuduNetwork)
                .withNetworkAliases(KUDU_MASTER_NETWORK_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withHostName(KUDU_TABLET_NETWORK_ALIAS);
                })
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
        tabletContainer = new GenericContainer<>(new ImageFromDockerfile()
                .withDockerfile(Path.of(this.getClass().getResource("/kerby/Dockerfile").getFile())))
                .withCommand("tserver")
                .withExposedPorts(KUDU_MASTER_RPC_PORT, KUDU_MASTER_HTTP_PORT)
                .withEnv("TSERVER_ARGS", "--unlock_unsafe_flags=true " +
                        "--rpc_authentication=required " +
                        "--keytab_file=/home/kudu/" + KUDU_TABLET_NETWORK_ALIAS + " " +
                        "--allow_world_readable_credentials=true " + //https://kudu.apache.org/docs/prior_release_notes.html
                        "--stderrthreshold=0 ")
                .withCopyToContainer(MountableFile.forClasspathResource("kerby/krb5.conf"),
                        "/etc/krb5.conf")
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
        } catch (KrbException ex) {
            LOG.error("An issue occurred while stopping the KerbyServer.", ex);
        }
    }
}
