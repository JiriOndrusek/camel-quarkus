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

package org.apache.camel.quarkus.component.cxf.soap.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class CxfClientTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int WILDFLY_PORT = 8080;
    private GenericContainer<?> helloContainer;

    @Override
    public Map<String, String> start() {

        try {
            helloContainer = new GenericContainer<>("cxf-soap/hello:1.0")
                    .withExposedPorts(WILDFLY_PORT)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("config.cli"), "/opt/server/bin/config.cli")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("example-users.properties"),
                            "/opt/server/standalone/configuration/example-users.properties")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("example-roles.properties"),
                            "/opt/server/standalone/configuration/example-roles.properties")
                    .waitingFor(Wait.forHttp("/hello-ws/HelloService?wsdl"));

            //            helloContainer.start();
            //
            //            Container.ExecResult res = helloContainer.execInContainer("sh", "/opt/server/bin/jboss-cli.sh", "-c",
            //                    "--file=/opt/server/bin/config.cli");
            //            if (res.getExitCode() != 0) {
            //                throw new IllegalArgumentException("Configuration of wildfly server failed. " + res.getStdout());
            //            }

            return Map.of(
                    "camel-quarkus.it.helloWorld.baseUri",
                    "http://" + helloContainer.getHost()
                            + ":8080");
            //                            + ":" + helloContainer.getMappedPort(WILDFLY_PORT));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (helloContainer != null) {
                helloContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
