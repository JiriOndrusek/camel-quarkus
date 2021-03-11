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

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.containers.GenericContainer;

public class SplunkTestResource implements QuarkusTestResourceLifecycleManager {

    public static String SUBMIT_INDEX = "submitindex";
    public static String STREAM_INDEX = "streamindex";
    public static String NORMAL_SEARCH_INDEX = "normalindex";
    public static String SAVED_SEARCH_INDEX = "savedindex";
    public static String REALTIME_SEARCH_INDEX = "realtimeindex";
    public static String TCP_INDEX = "tcpindex";
    public static String SAVED_SEARCH_NAME = "savedSearchForTest";
    private static final int REMOTE_PORT = 8089;
    private static final int TCP_PORT = 9997;

    private GenericContainer container;

    @Override
    public Map<String, String> start() {

        try {
                        container = new GenericContainer("splunk/splunk:8.1.2")
                                .withExposedPorts(REMOTE_PORT)
                                .withEnv("SPLUNK_START_ARGS", "--accept-license")
                                .withEnv("SPLUNK_PASSWORD", "changeit")
                                .withEnv("SPLUNK_LICENSE_URI", "Free")
                                .withStartupTimeout(Duration.ofSeconds(120))
                                .waitingFor(
                                        Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1));

                        container.start();

                        container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/",
                                "/opt/splunk/etc/system/default/server.conf");
                        container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/",
                                "/opt/splunk/etc/system/default/server.conf");
                        container.execInContainer("sudo", "./bin/splunk", "restart");
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", SUBMIT_INDEX);
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", NORMAL_SEARCH_INDEX);
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", REALTIME_SEARCH_INDEX);
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", SAVED_SEARCH_INDEX);
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", STREAM_INDEX);
                        container.execInContainer("sudo", "./bin/splunk", "add", "index", TCP_INDEX);

                        return CollectionHelper.mapOf(
                                SplunkResource.PARAM_REMOTE_PORT, container.getMappedPort(REMOTE_PORT).toString(),
                                SplunkResource.PARAM_TCP_PORT, container.getMappedPort(TCP_PORT).toString());

//            return CollectionHelper.mapOf(
//                    SplunkResource.PARAM_REMOTE_PORT, "32908",
//                    SplunkResource.PARAM_TCP_PORT, "32905");
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
