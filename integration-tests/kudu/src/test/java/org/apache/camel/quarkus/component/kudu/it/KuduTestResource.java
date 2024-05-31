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

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.kudu.test.KuduTestHarness;
import org.apache.kudu.test.cluster.MiniKuduCluster;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import static org.apache.camel.quarkus.component.kudu.it.KuduInfrastructureTestHelper.DOCKER_HOST;
import static org.apache.camel.quarkus.component.kudu.it.KuduInfrastructureTestHelper.MASTER_URL;
import static org.apache.camel.quarkus.component.kudu.it.KuduRoute.KUDU_AUTHORITY_CONFIG_KEY;


@CreateKdcServer(
        transports =
                {
                        @CreateTransport(protocol = "UDP", port = 6088),
                        @CreateTransport(protocol = "TCP", port = 6088)
                })
public class KuduTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(KuduTestResource.class);

    private KuduTestHarness kuduTestHarness;

    private static final String REALM = "EXAMPLE.COM";
    private static final String PRINCIPAL = "kuduuser@EXAMPLE.COM";
    private static final String KEYTAB_PATH = "/path/to/testuser.keytab";
    private KdcServer kdcServer;
    DirectoryService directoryService;


    @Override
    public Map<String, String> start() {

        MiniKuduCluster.MiniKuduClusterBuilder cb = new MiniKuduCluster.MiniKuduClusterBuilder()
                .numMasterServers(1)
                .numTabletServers(1);
        cb.enableKerberos();
        cb.principal("kuduuser");

        kuduTestHarness = new KuduTestHarness(cb);
        try {
            directoryService = startDirectoryService();

            CreateKdcServer createKdcServer = KuduTestResource.class.getAnnotation(CreateKdcServer.class);
            KerberosConfig kdcConfig = new KerberosConfig();
            kdcConfig.setServicePrincipal(createKdcServer.kdcPrincipal());
            kdcConfig.setPrimaryRealm(createKdcServer.primaryRealm());
            kdcConfig.setMaximumTicketLifetime(createKdcServer.maxTicketLifetime());
            kdcConfig.setMaximumRenewableLifetime(createKdcServer.maxRenewableLifetime());
            KdcServer kdcServer = new KdcServer(kdcConfig);
            kdcServer.setSearchBaseDn(createKdcServer.searchBaseDn());
            kdcServer.setDirectoryService(directoryService);
            kdcServer.addTransports(new UdpTransport("localhost", 6088));
            kdcServer.addTransports(new TcpTransport("localhost", 6088, 3, 50));
            kdcServer.start();

            startKerberosServer();

            kuduTestHarness.before();


            kuduTestHarness.kinit("kuduuser");

//            LOG.info("Kudu master RPC accessible at " + kuduTestHarness.getMasterAddressesAsString());

//            return CollectionHelper.mapOf(
//                    KUDU_AUTHORITY_CONFIG_KEY, kuduTestHarness.getMasterAddressesAsString(),
//                    DOCKER_HOST, DockerClientFactory.instance().dockerHostIpAddress(),
//                    MASTER_URL, kuduTestHarness.getMasterAddressesAsString());
            return CollectionHelper.mapOf(
                    KUDU_AUTHORITY_CONFIG_KEY, "xx",
                    DOCKER_HOST, "xxx",
                    MASTER_URL, "xxx");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DirectoryService startDirectoryService() throws Exception {
        DefaultDirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(KuduTestResource.class.getSimpleName());
        return dsf.getDirectoryService();
    }

    private void startKerberosServer() throws Exception {
//        directoryService = DirectoryServiceFactory.createDirectoryService("default");
//        directoryService.getChangeLog().setEnabled(false);
//        directoryService.setAllowAnonymousAccess(false);
//        directoryService.startup();

//        KerberosConfig config = new KerberosConfig();
//        config.setServiceName("KDC Server");
//        config.setSearchBaseDn("ou=system");
//        config.setPrimaryRealm(REALM);
//        config.setServicePrincipal("krbtgt/" + REALM + "@" + REALM);
//        kdcServer = new KdcServer(config);
//        kdcServer.addTransports(new UdpTransport(60088));
//        kdcServer.setDirectoryService(directoryService);
//        kdcServer.start();

        // Add user principal
        // Create a new user principal and generate keytab for it
        // (You need to implement addPrincipal and generateKeytab methods based on your setup)
//        addPrincipal(PRINCIPAL, "password");
//        generateKeytab(PRINCIPAL, KEYTAB_PATH);
    }

    @Override
    public void stop() {
        if (kuduTestHarness != null) {
            try {
                if (kuduTestHarness != null) {
                    kuduTestHarness.after();
                }
            } catch (Exception ex) {
                LOG.error("An issue occurred while stopping the KuduTestResource", ex);
            }
        }
    }

    @Override
    public void inject(Object testInstance) {
        QuarkusTestResourceLifecycleManager.super.inject(testInstance);

        if(testInstance instanceof KuduTest) {
            ((KuduTest) testInstance).setClient(kuduTestHarness.getClient());
        }
    }
}


