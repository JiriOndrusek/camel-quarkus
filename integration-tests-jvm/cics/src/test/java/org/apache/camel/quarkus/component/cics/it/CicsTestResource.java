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

package org.apache.camel.quarkus.component.cics.it;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.certificate.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

public class CicsTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(CicsTestResource.class);
    //    private static final String CICS_IMAGE = ConfigProvider.getConfig().getValue("kudu.container.image", String.class);
    private static final String CICS_IMAGE = "images.paas.redhat.com/fuseqe/ibm-cicstg-container-linux-x86-trial:9.3";

    private final static Network network = Network.newNetwork();

    private GenericContainer container, haProxy;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(CICS_IMAGE)
                    .withEnv("LICENSE", "accept")
                    //                    .withNetwork(network)
                    //                            .withNetworkMode("host")
                    //                            .withNetworkAliases("cgt")
                    .withExposedPorts(8573, 2810)
                    .withLogConsumer(new Slf4jLogConsumer(LOG))
                    .withCopyToContainer(MountableFile.forHostPath(CertificatesUtil.keystoreFile("localhost", "p12")),
                            "/home/ctg/config/server.keystore")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("ctg.ini"), "/var/cicscli/ctg.ini")
                    .waitingFor(Wait.forLogMessage(".*CTG6512I CICS Transaction Gateway initialization complete.*", 1))
                    .withStartupTimeout(Duration.ofSeconds(60L));
            //            container.start();
            //
            //            //start reverse proxy for ssl testing
            //            haProxy = new GenericContainer("haproxytech/haproxy-alpine:latest")
            //                    //                    .withNetwork(network)
            //                    .withNetworkMode("host")
            //                    .withNetworkAliases("haProxy")
            //                    .withExposedPorts(8080, 8404)
            //                    .withLogConsumer(new Slf4jLogConsumer(LOG))
            //                    .withCopyToContainer(MountableFile.forHostPath("/haproxy.cfg"),
            //                            "/usr/local/etc/haproxy/haproxy.cfg")
            //                    .waitingFor(Wait.forLogMessage(".*Loading success.*", 1));
            //            haProxy.start();

            //        //copy docker-compose to tmp location
            //        File dockerComposeFile, haproxyCfgFile, ctgIniFile, serverKeystoreFile;
            //        try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("compose.yaml");
            //                InputStream ctgIni = getClass().getClassLoader().getResourceAsStream("ctg.ini");
            //                InputStream serverKeystore = new FileInputStream("target/certs/localhost-keystore.p12");
            //                InputStream haproxyCfg = getClass().getClassLoader().getResourceAsStream("haproxy.cfg");) {
            //
            //            dockerComposeFile = File.createTempFile("ctg-compose-", ".yaml");
            //            haproxyCfgFile = File.createTempFile("haproxy-", ".cfg");
            //            ctgIniFile = File.createTempFile("ctg-", ".ini");
            //            serverKeystoreFile = File.createTempFile("localhost-", ".p12");
            //            Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //            Files.copy(haproxyCfg, haproxyCfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //            Files.copy(ctgIni, ctgIniFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //            Files.copy(serverKeystore, serverKeystoreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //
            //            container = new ComposeContainer(dockerComposeFile)
            //                    .withExposedService("haproxy", 8404)
            //                    .withExposedService("ctg", 8573)
            //                    .withEnv("HAPROXY_CFG_FILE", haproxyCfgFile.getAbsolutePath())
            //                    .withEnv("CTG_INI_FILE", ctgIniFile.getAbsolutePath())
            //                    .withEnv("SERVER_KEYSTORE_FILE", serverKeystoreFile.getAbsolutePath())
            //                    .withLogConsumer("ctg", new Slf4jLogConsumer(LOG))
            //                    .withLogConsumer("haproxy", new Slf4jLogConsumer(LOG))
            //                    .waitingFor("haproxy", Wait.forLogMessage(".*Loading success.*", 1));
            container.start();
            return Map.of("tcg.tcp.port", container.getMappedPort(8573) + "",
                    "ctg.host", container.getHost());
            //            return Map.of("tcg.tcp.port", container.getServicePort("ctg", 8573) + "",
            //                                return Map.of("tcg.tcp.port", container.getServicePort("haproxy", 8404) + "",
            //                    "ctg.host", container.getServiceHost("haproxy", 8404));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
            if (haProxy != null) {
                haProxy.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occurred while stopping the CicsTestResource", ex);
        }
    }
}
