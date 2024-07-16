package org.apache.camel.quarkus.component.kudu.it.kerby;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.file.Path;

import org.apache.camel.quarkus.component.kudu.it.IpAddressHelper;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;

public class KerbyServer {
    private SimpleKdcServer kdcServer;

    public void startServer(String workDir) throws KrbException, IOException {
        kdcServer = new SimpleKdcServer();
        kdcServer.setWorkDir(new File(workDir));
        kdcServer.setKdcHost(IpAddressHelper.getHost4Address());
        kdcServer.setKdcRealm("EXAMPLE.COM");
        kdcServer.setKdcUdpPort(10089);
        kdcServer.setKdcTcpPort(35202);
        kdcServer.init();
        kdcServer.start();
    }

    public void stopServer() throws KrbException {
        if (kdcServer != null) {
            kdcServer.stop();
        }
    }

    public void createPrincipal(String name, String principal, String password) throws KrbException {
        kdcServer.createPrincipal(principal, password);
        kdcServer.exportPrincipal(principal, Path.of(kdcServer.getClass().getResource("/kerby").getFile(), name).toFile());
    }
}
