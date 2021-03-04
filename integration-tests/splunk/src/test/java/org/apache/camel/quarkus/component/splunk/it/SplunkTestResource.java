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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SplunkTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int REMOTE_PORT = 8089;

    private GenericContainer container;

    @Override
    public Map<String, String> start() {

        try {
            container = new GenericContainer("splunk/splunk:latest")
                    .withExposedPorts(REMOTE_PORT)
                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
                    .withEnv("SPLUNK_PASSWORD", "changeit")
//                    .withEnv("SPLUNK_INDEXER_URL", "junit")
                    .withEnv("SPLUNK_LICENSE_URI", "Free")
//                    .withCommand("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/default/server.conf")
                    .waitingFor(Wait.forListeningPort());
//            ;

            WaitingConsumer wait = new WaitingConsumer();
            final ToStringConsumer toString = new ToStringConsumer();

//            container = new GenericContainer(
//                    new ImageFromDockerfile()
//                            .withDockerfileFromBuilder(builder ->
//                                    builder
//                                            .from("splunk/splunk:latest")
//                                            .cmd("ls /sbin")
//                                            .cmd("/bin/bash", "cat /opt/splunk/etc/system/default/server.conf")
//                                            .run("echo '**********111111111111111111111*****************'")
//                                            .run("touch", "tmp.txt")
//                                            .run("sudo sed -i 's/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always' /opt/splunk/etc/system/default/server.conf")
//                                            .entryPoint("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/default/server.conf")
//                                            .entryPoint("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/default/server.conf")
//                                    entryPoint("/sbin/entrypoint.sh start")
//                                            .entryPoint("/bin/sh", "-c",  "sudo sed -i 's/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/' /opt/splunk/etc/system/default/server.conf && /sbin/entrypoint.sh start")
//                                            .build()))
//                                    .withCommand("/bin/bash","touch", "tmp.txt")
//                    .withExposedPorts(REMOTE_PORT)
//                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
//                    .withEnv("SPLUNK_PASSWORD", "changeit")
//                    .withEnv("SPLUNK_LICENSE_URI", "Free");
//                    .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
//                    .withLogConsumer(wait.andThen(toString));

            container.start();

//            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/default/server.conf");
            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/default/server.conf");
            container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/", "/opt/splunk/etc/system/default/server.conf");
//            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/", "/opt/splunk/etc/system/local/server.conf");
            container.execInContainer("sudo", "./bin/splunk stop");
            Thread.sleep(60000);
            container.execInContainer("sudo", "./bin/splunk start");
            Thread.sleep(60000);
//            container.execInContainer("sudo", "/sbin/entrypoint.sh restart");

//
//            container.stop();
//            container.start();

            return CollectionHelper.mapOf(
                    "remotePort", container.getMappedPort(REMOTE_PORT).toString());
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
