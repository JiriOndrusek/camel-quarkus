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
package org.apache.camel.quarkus.test.support.splunk;

import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class SplunkTestResource implements QuarkusTestResourceLifecycleManager {

    public static String TEST_INDEX = "testindex";
    public static final String HEC_TOKEN = "TESTTEST-TEST-TEST-TEST-TESTTESTTEST";

    private static final String SPLUNK_IMAGE_NAME = ConfigProvider.getConfig().getValue("splunk.container.image", String.class);
    private static final int REMOTE_PORT = 8089;
    private static final int WEB_PORT = 8000;
    private static final int HEC_PORT = 8088;
    private static final Logger LOG = LoggerFactory.getLogger(SplunkTestResource.class);

    private GenericContainer<?> container;

    private boolean ssl;

    @Override
    public void init(Map<String, String> initArgs) {
        ssl = Boolean.parseBoolean(initArgs.getOrDefault("ssl", "false"));
    }

    @Override
    public Map<String, String> start() {

        try {
            container = new GenericContainer<>(SPLUNK_IMAGE_NAME)
                    .withExposedPorts(REMOTE_PORT, SplunkConstants.TCP_PORT, WEB_PORT, HEC_PORT)
                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
                    .withEnv("SPLUNK_PASSWORD", "changeit")
                    .withEnv("SPLUNK_HEC_TOKEN", HEC_TOKEN)
                    .withEnv("SPLUNK_LICENSE_URI", "Free")
                    .withEnv("TZ", TimeZone.getDefault().getID())
                    //                    .withLogConsumer(new Slf4jLogConsumer(LOG))
                    .waitingFor(
                            Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

            if (ssl) {
                container.withCopyToContainer(MountableFile.forClasspathResource("certs/splunk.crt"),
                        "/tmp/defaults/server.pem")
                        .withCopyToContainer(MountableFile.forClasspathResource("certs/splunk-ca.crt"),
                                "/tmp/defaults/cacert.pem");
            }

            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
            LOG.info(">>>>>>>>>>> starting with ssl: " + ssl + ">>>>>>>>>.");
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");

            container.start();

            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/",
                    "/opt/splunk/etc/system/default/server.conf");
            container.execInContainer("sudo", "sed", "-i", "s/minFreeSpace = 5000/minFreeSpace = 100/",
                    "/opt/splunk/etc/system/default/server.conf");

            if (ssl) {
                container.execInContainer("sudo", "sed", "-i",
                        "s,serverCert = $SPLUNK_HOME\\/etc\\/auth\\/server.pem,serverCert =  \\/tmp\\/defaults\\/server.pem",
                        "/opt/splunk/etc/system/default/server.conf");
                container.execInContainer("sudo", "sed", "-i",
                        "s,caCertFile = $SPLUNK_HOME\\/etc\\/auth\\/cacert.pem,caCertFile = \\/tmp\\/defaults\\/cacert.pem",
                        "/opt/splunk/etc/system/default/server.conf");
            } else {
                container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/",
                        "/opt/splunk/etc/system/default/server.conf");
            }

            container.execInContainer("sudo", "microdnf", "--nodocs", "update", "tzdata");//install tzdata package so we can specify tz other than UTC

            container.execInContainer("sudo", "./bin/splunk", "restart");
            container.execInContainer("sudo", "./bin/splunk", "add", "index", TEST_INDEX);
            container.execInContainer("sudo", "./bin/splunk", "add", "tcp", String.valueOf(SplunkConstants.TCP_PORT),
                    "-sourcetype", "TCP");

            String splunkHost = container.getHost();

            String paramRemotePort = ssl ? SplunkSslConstants.PARAM_REMOTE_PORT : SplunkConstants.PARAM_REMOTE_PORT;
            String paramHecPort = ssl ? SplunkSslConstants.PARAM_HEC_PORT : SplunkConstants.PARAM_HEC_PORT;
            String paramTcpPort = ssl ? SplunkSslConstants.PARAM_TCP_PORT : SplunkConstants.PARAM_TCP_PORT;

            Map<String, String> m = Map.of(
                    SplunkConstants.PARAM_REMOTE_HOST, splunkHost,
                    paramTcpPort, container.getMappedPort(SplunkConstants.TCP_PORT).toString(),
                    SplunkConstants.PARAM_HEC_TOKEN, HEC_TOKEN,
                    SplunkConstants.PARAM_TEST_INDEX, TEST_INDEX,
                    paramRemotePort, container.getMappedPort(REMOTE_PORT).toString(),
                    paramHecPort, container.getMappedPort(HEC_PORT).toString());

            String banner = StringUtils.repeat("*", 50);
            LOG.info(banner);
            LOG.info(String.format("Splunk UI running on: http://%s:%d", splunkHost, container.getMappedPort(WEB_PORT)));
            LOG.info(m.toString());
            LOG.info(banner);

            return m;

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
        } catch (Exception e) {
            // Ignored
        }
    }
}
