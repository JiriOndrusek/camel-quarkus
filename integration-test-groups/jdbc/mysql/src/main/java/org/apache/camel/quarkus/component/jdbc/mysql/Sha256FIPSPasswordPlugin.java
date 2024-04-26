package org.apache.camel.quarkus.component.jdbc.mysql;

import com.mysql.cj.protocol.a.authentication.Sha256PasswordPlugin;

public class Sha256FIPSPasswordPlugin extends Sha256PasswordPlugin {

    public Sha256FIPSPasswordPlugin() {
        super();
    }

    @Override
    public String getProtocolPluginName() {
        return "cq_fips_plugin";
    }

    @Override
    protected byte[] encryptPassword() {
        return encryptPassword("RSA/ECB/PKCS1Padding");
    }
}
