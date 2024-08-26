package org.apache.camel.quarkus.component.kudu.it;

import org.apache.camel.quarkus.component.kudu.it.kerby.KerbyServer;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.slf4j.Logger;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class Slf4jCustomLogConsumer extends Slf4jLogConsumer {

    private KerbyServer kerbyServer;

    public Slf4jCustomLogConsumer(Logger logger, KerbyServer kerbyServer) {
        super(logger);
        this.kerbyServer = kerbyServer;
    }

    public Slf4jCustomLogConsumer(Logger logger, boolean separateOutputStreams) {
        super(logger, separateOutputStreams);
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        String s = outputFrame.getUtf8String();
//        if(s.contains("@EXAMPLE.COM")) {
//            System.out.println(s);
//        }
//        if(s.contains("@EXAMPLE.COM not found in Kerberos database")) {
//            s = s.substring(0, s.indexOf("@EXAMPLE.COM not found in Kerberos database"));
//            String missingPrincipal = s.substring(s.lastIndexOf(" ") + 1);
//            try {
//                kerbyServer.createPrincipal(missingPrincipal, "changeit");
//            } catch (KrbException e) {
//                throw new RuntimeException(e);
//            }
//        }
        super.accept(outputFrame);
    }
}
