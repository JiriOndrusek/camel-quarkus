package org.apache.camel.quarkus.component.kudu.it.kerby;

import java.io.File;

import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.client.KrbConfig;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;

public class KerbyClient {
    private KrbClient krbClient;

    public KerbyClient(String confDir) throws Exception {
        krbClient = new KrbClient(new File(confDir));
        krbClient.setKdcHost("localhost");
        krbClient.setKdcRealm("EXAMPLE.COM");

        KrbConfig krbConfig = new KrbConfig();
        File confFile = new File(confDir + "/krb5.conf");
        krbConfig.addKrb5Config(confFile);

        krbClient.init();
    }

    public TgtTicket authenticate(String principal, String password) {
        try {
            return krbClient.requestTgt(principal, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public KrbClient getKrbClient() {
        return krbClient;
    }
}
