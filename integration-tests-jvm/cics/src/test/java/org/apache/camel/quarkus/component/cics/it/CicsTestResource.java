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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.certificate.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class CicsTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(CicsTestResource.class);
    //    private static final String CICS_IMAGE = ConfigProvider.getConfig().getValue("kudu.container.image", String.class);
    private static final String CICS_IMAGE = "images.paas.redhat.com/fuseqe/ibm-cicstg-container-linux-x86-trial:9.3";

    private final static Network network = Network.newNetwork();

    private ComposeContainer container, haProxy;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        //        try {
        //            container = new GenericContainer(CICS_IMAGE)
        //                    .withEnv("LICENSE", "accept")
        //                    //                    .withNetwork(network)
        //                    .withNetworkMode("host")
        //                    .withNetworkAliases("cgt")
        //                    .withExposedPorts(2006, 2810)
        //                    .withLogConsumer(new Slf4jLogConsumer(LOG))
        //                    //                    .withCopyToContainer(MountableFile.forHostPath(CertificatesUtil.keystoreFile("ctg-server", "p12")),
        //                    //                            "/home/ctg/config/server.keystore")
        //                    .withCopyFileToContainer(MountableFile.forClasspathResource("ctg.ini"), "/var/cicscli/ctg.ini")
        //                    .waitingFor(Wait.forLogMessage(".*CTG6512I CICS Transaction Gateway initialization complete.*", 1))
        //                    .withStartupTimeout(Duration.ofSeconds(60L));
        //            //            container.start();
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

        //copy docker-compose to tmp location
        File dockerComposeFile, haproxyCfgFile, ctgIniFile, serverKeystoreFile;
        try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("compose.yaml");
                InputStream ctgIni = getClass().getClassLoader().getResourceAsStream("ctg.ini");
                InputStream serverKeystore = new FileInputStream(CertificatesUtil.keystoreFile("ctg-server", "p12"));
                InputStream haproxyCfg = getClass().getClassLoader().getResourceAsStream("haproxy.cfg");) {

            dockerComposeFile = File.createTempFile("ctg-compose-", ".yaml");
            haproxyCfgFile = File.createTempFile("haproxy-", ".cfg");
            ctgIniFile = File.createTempFile("ctg-", ".ini");
            serverKeystoreFile = File.createTempFile("server-keystore-", ".p12");
            Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(haproxyCfg, haproxyCfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(ctgIni, ctgIniFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(serverKeystore, serverKeystoreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            container = new ComposeContainer(dockerComposeFile)
                    .withExposedService("haproxy", 8404)
                    .withExposedService("ctg", 2006)
                    .withEnv("HAPROXY_CFG_FILE", haproxyCfgFile.getAbsolutePath())
                    .withEnv("CTG_INI_FILE", ctgIniFile.getAbsolutePath())
                    .withEnv("SERVER_KEYSTORE_FILE", serverKeystoreFile.getAbsolutePath())
                    .withLogConsumer("ctg", new Slf4jLogConsumer(LOG))
                    .withLogConsumer("haproxy", new Slf4jLogConsumer(LOG))
                    .waitingFor("haproxy", Wait.forLogMessage(".*Loading success.*", 1));
            container.start();
            //            return Map.of("tcg.tcp.port", container.getMappedPort(2006) + "",
            return Map.of("tcg.tcp.port", container.getServicePort("ctg", 2006) + "",
                    //            return Map.of("tcg.tcp.port", container.getServicePort("haproxy", 8404) + "",
                    "ctg.host", container.getServiceHost("haproxy", 8404));
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
