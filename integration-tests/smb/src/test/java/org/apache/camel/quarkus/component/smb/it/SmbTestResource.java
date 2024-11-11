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
package org.apache.camel.quarkus.component.smb.it;

import java.io.IOException;
import java.util.Map;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SmbTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int SMB_PORT = 445;

    private GenericContainer<?> container;

    private static final String SMB_IMAGE = ConfigProvider.getConfig().getValue("smb.container.image", String.class);

    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer<>(SMB_IMAGE)
                    .withExposedPorts(SMB_PORT)
                    .waitingFor(Wait.forListeningPort());
            container.start();

            String smbHost = container.getHost();
            int smbPort = container.getMappedPort(SMB_PORT);

            //create folder test
            SMBClient client = new SMBClient();


//            try (Connection connection = client.connect(smbHost, smbPort)) {
//                // Authenticate with SMB server
//                AuthenticationContext ac = new AuthenticationContext("camel", "camelTester123".toCharArray(), null);
//                Session session = connection.authenticate(ac);
//
//                // Connect to the shared folder
//                DiskShare share = (DiskShare) session.connectShare("data-rw");
//
//                // Check if the folder exists, and create if it doesn't
//                if (!share.folderExists("test")) {
//                    share.mkdir("test");
//                    System.out.println("Folder created: test");
//                } else {
//                    System.out.println("Folder already exists:  test");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            return Map.of(
                    "smb.host", smbHost,
                    "smb.port", Integer.toString(smbPort),
                    "smb.share", "data-rw",
                    "smb.username", "camel",
                    "smb.password", "camelTester123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
