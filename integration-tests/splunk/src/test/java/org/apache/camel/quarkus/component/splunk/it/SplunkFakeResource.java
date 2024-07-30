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
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class SplunkFakeResource implements QuarkusTestResourceLifecycleManager {

    public static String TEST_INDEX = "testindex";
    public static final String HEC_TOKEN = "TESTTEST-TEST-TEST-TEST-TESTTESTTEST";

    private static final String SPLUNK_IMAGE_NAME = ConfigProvider.getConfig().getValue("splunk.container.image", String.class);
    private static final int REMOTE_PORT = 8089;
    private static final int WEB_PORT = 8000;
    private static final int HEC_PORT = 8088;
    private static final Logger LOG = LoggerFactory.getLogger(SplunkFakeResource.class);

    private GenericContainer<?> container;

    private boolean ssl;

    @Override
    public void init(Map<String, String> initArgs) {
        ssl = Boolean.parseBoolean(initArgs.getOrDefault("ssl", "false"));
    }

    @Override
    public Map<String, String> start() {

        try {

            Map<String, String> m = Map.of(
                    SplunkConstants.PARAM_REMOTE_HOST, "localhost",
                    //                    SplunkConstants.PARAM_TCP_PORT, "32871", //ssl
                    SplunkConstants.PARAM_TCP_PORT, "32785", //no ssh
                    SplunkConstants.PARAM_TEST_INDEX, TEST_INDEX,
                    //                    SplunkConstants.PARAM_REMOTE_PORT, "32872"); //ssl
                    SplunkConstants.PARAM_REMOTE_PORT, "32786"); //no ssh

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

    @Override
    public void stop() {

    }

}
