package org.apache.camel.quarkus.component.kudu.it.kerby;

import java.io.File;
import java.io.IOException;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;

public class KerbyServer {
    private SimpleKdcServer kdcServer;

    public void startServer(String workDir) throws KrbException, IOException {
        kdcServer = new SimpleKdcServer();
        kdcServer.setWorkDir(new File(workDir));
        kdcServer.setKdcHost("localhost");
        kdcServer.setKdcRealm("EXAMPLE.COM");
        kdcServer.setKdcPort(60088); // Non-standard port to avoid conflicts
        kdcServer.init();
        kdcServer.start();
    }

    public void stopServer() throws KrbException {
        if (kdcServer != null) {
            kdcServer.stop();
        }
    }

    public void createPrincipal(String workDir, String principal, String password) throws KrbException {
        kdcServer.createAndExportPrincipals(new File(workDir, "keytab"), principal, password);
    }
}
