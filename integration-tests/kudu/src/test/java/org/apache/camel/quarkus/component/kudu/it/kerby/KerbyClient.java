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
        //        krbClient.setKdcUdpPort(60088); // Non-standard port to match the server

        KrbConfig krbConfig = new KrbConfig();
        //todo
        File confFile = new File(confDir + "/krb5.conf");
        krbConfig.addKrb5Config(confFile);

        krbClient.init();
    }

    public boolean authenticate(String principal, String password) {
        try {
            //            KOptions options = new KOptions();
            //            options.add("principal", principal);
            //            options.add("password", password);

            TgtTicket token = krbClient.requestTgt(principal, password);

            //            Subject subject = JaasKrbUtil.loginUsingTicketCache(principal, token.getToken());
            return token != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
