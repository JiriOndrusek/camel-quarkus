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
package org.apache.camel.quarkus.component.splunk.it;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
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
//            container = new GenericContainer<>(SPLUNK_IMAGE_NAME)
//                    .withExposedPorts(REMOTE_PORT, SplunkConstants.TCP_PORT, WEB_PORT, HEC_PORT)
//                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
//                    .withEnv("SPLUNK_PASSWORD", "changeit")
//                    .withEnv("SPLUNK_HEC_TOKEN", HEC_TOKEN)
//                    .withEnv("SPLUNK_LICENSE_URI", "Free")
//                    .withEnv("SPLUNK_USER", "root")//does not work
//                    .withEnv("TZ", TimeZone.getDefault().getID())
//                    .withLogConsumer(new Slf4jLogConsumer(LOG))
//                    .waitingFor(
//                            Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1)
//                                    .withStartupTimeout(Duration.ofMinutes(5)));
//
//            if (ssl) {
//                //how to combine server.pem
//                //openssl pkcs12 -export -out combined.p12 -inkey private.key -in certificate.crt -certfile ca-cert.crt
//                //openssl pkcs12 -in combined.p12 -out combined.pem -nodes
//
//                //                ..verification openssl s_client -showcerts -connect localhost:32835
//                //                https://blog.packagecloud.io/solve-unable-to-find-valid-certification-path-to-requested-target/
//                container.withCopyToContainer(MountableFile.forClasspathResource("keytool/combined.pem"),
//                        "/opt/splunk/etc/auth/mycerts/myServerCert.pem")
//                        //                        .withCopyToContainer(MountableFile.forClasspathResource("ssh_default.conf"),
//                        //                                "/opt/splunk/etc/system/local/server.conf")
//                        .withCopyToContainer(MountableFile.forClasspathResource("keytool/splunkca.pem"),
//                                "/opt/splunk/etc/auth/mycerts/cacert.pem");
//                //                        .withCreateContainerCmdModifier(cmd -> {
//                //                            cmd.
//                //                        })
//            }
//
//            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
//            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
//            LOG.info(">>>>>>>>>>> starting with ssl: " + ssl + ">>>>>>>>>.");
//            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
//            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.");
//            container.start();
//
//            //I'm getting this issue when using local.conf https://community.splunk.com/t5/Splunk-Enterprise-Security/Getting-error-0906D06C-PEM-routines-PEM-read-bio-no-start-line/m-p/560349
//            LOG.info("****************** changing config *************************");
//            LOG.info("****************** changing config *************************");
//
//            //modify local conf according to https://docs.splunk.com/Documentation/Splunk/9.2.0/Security/ConfigTLSCertsS2S
//            //remove password
//            container.copyFileToContainer(MountableFile.forClasspathResource("local_server.conf"),
//                    "/opt/splunk/etc/system/local/server.conf");
//            //copy conf from the container to see the result
//            container.copyFileFromContainer("/opt/splunk/etc/system/local/server.conf",
//                    Path.of(getClass().getResource("/").getPath()).resolve("local_server.conf").toFile()
//                            .getAbsolutePath());
//
//            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/",
//                    "/opt/splunk/etc/system/local/server.conf");
//            container.execInContainer("sudo", "sed", "-i", "s/minFreeSpace = 5000/minFreeSpace = 100/",
//                    "/opt/splunk/etc/system/local/server.conf");
//
//            if (ssl) {
//                //                asserExecResult(container.execInContainer("sudo", "sed", "-i",
//                //                        "s,serverCert = $SPLUNK_HOME\\/etc\\/auth\\/server.pem,serverCert = \\/tmp\\/defaults\\/server.pem,",
//                //                        "/opt/splunk/etc/system/default/server.conf"), "sed (serverCert)");
//                //
//                //                asserExecResult(container.execInContainer("sudo", "sed", "-i",
//                //                        "s,caCertFile = $SPLUNK_HOME\\/etc\\/auth\\/cacert.pem,caCertFile = \\/tmp\\/defaults\\/cacert.pem,",
//                //                        "/opt/splunk/etc/system/default/server.conf"), "sed (cacertfile)");
//
//                //export pems from the splunk
//                container.copyFileFromContainer("/opt/splunk/etc/auth/cacert.pem",
//                        Path.of(getClass().getResource("/").getPath()).resolve("cacert_from_container.pem").toFile()
//                                .getAbsolutePath());
//                container.copyFileFromContainer("/opt/splunk/etc/auth/server.pem",
//                        Path.of(getClass().getResource("/").getPath()).resolve("server_from_container.pem").toFile()
//                                .getAbsolutePath());
//                container.copyFileFromContainer("/opt/splunk/etc/system/local/server.conf",
//                        Path.of(getClass().getResource("/").getPath()).resolve("local_server.conf").toFile()
//                                .getAbsolutePath());
//
//            } else {
//                asserExecResult(
//                        container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/",
//                                "/opt/splunk/etc/system/default/server.conf"),
//                        "disabling ssl");
//            }
//
//            container.execInContainer("sudo", "microdnf", "--nodocs", "update", "tzdata");//install tzdata package so we can specify tz other than UTC
//
//            LOG.info("****************** restarting *************************");
//            asserExecResult(container.execInContainer("sudo", "./bin/splunk", "restart"), "splunk restart");
//
//            /*
//            container.execInContainer("sudo", "./bin/splunk", "add", "index", TEST_INDEX);
//            container.execInContainer("sudo", "./bin/splunk", "add", "tcp", String.valueOf(SplunkConstants.TCP_PORT),
//                    "-sourcetype", "TCP");
//             */
//
//            //            //final conf file is copyied from the container for the manual verification purposes
//            //            container.copyFileFromContainer("/opt/splunk/etc/system/default/server.conf",
//            //                    Path.of(getClass().getResource("/").getPath()).resolve("server.conf").toFile().getAbsolutePath());
//
//            String splunkHost = container.getHost();

            Map<String, String> m = Map.of(
                    SplunkConstants.PARAM_REMOTE_HOST, "localhost",
//                    SplunkConstants.PARAM_TCP_PORT, "32871", //ssl
                    SplunkConstants.PARAM_TCP_PORT, "32926", //no ssh
                    SplunkConstants.PARAM_TEST_INDEX, TEST_INDEX,
//                    SplunkConstants.PARAM_REMOTE_PORT, "32872"); //ssl
                    SplunkConstants.PARAM_REMOTE_PORT, "32927"); //no ssh

            String banner = StringUtils.repeat("*", 50);
            LOG.info(banner);
//            LOG.info(String.format("Splunk UI running on: http://localhost%s:%d", container.getMappedPort(WEB_PORT)));
            LOG.info(m.toString());
            LOG.info(banner);

            return m;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void asserExecResult(Container.ExecResult res, String cmd) {
        if (res.getExitCode() != 0) {
            LOG.error("Command: " + cmd);
            LOG.error("Stdout: " + res.getStdout());
            LOG.error("Stderr: " + res.getStderr());
            throw new RuntimeException("ommand sed (serverCert) failed. " + res.getStdout());
        } else {
            LOG.debug("Command: " + cmd + " succeeded!");
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
