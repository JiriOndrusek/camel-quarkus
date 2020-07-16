package org.apache.camel.quarkus.component.as2.it.util;

import org.apache.camel.component.as2.api.AS2ServerConnection;
import org.apache.camel.component.as2.api.AS2ServerManager;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.quarkus.component.as2.it.As2ClientTest;
import org.apache.http.protocol.HttpDateGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class As2ServerReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(As2ServerReceiver.class);

    private static final String SERVER_FQDN = "server.example.com";
    private static final String ORIGIN_SERVER_NAME = "AS2ClientManagerIntegrationTest Server";
    private static final String AS2_VERSION = "1.1";

    public static AS2ServerConnection serverConnection;
    public static KeyPair serverSigningKP;
    public static List<X509Certificate> serverCertList;

    private static final String MDN_FROM = "as2Test@server.example.com";
    private static final String MDN_SUBJECT_PREFIX = "MDN Response:";

    private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();

    public static void start() throws Exception {
        setupServerKeysAndCertificates();
        receiveTestMessages();
    }

    private static void setupServerKeysAndCertificates() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = Utils.makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        serverSigningKP = kpg.generateKeyPair();
        X509Certificate signingCert = Utils.makeCertificate(
                serverSigningKP, signingDN, issueKP, issueDN);

        serverCertList = new ArrayList<>();

        serverCertList.add(signingCert);
        serverCertList.add(issueCert);
    }

    private static void receiveTestMessages() throws IOException {
        serverConnection = new AS2ServerConnection(AS2_VERSION, ORIGIN_SERVER_NAME,
                SERVER_FQDN, Integer.parseInt(System.getProperty(As2ClientTest.PORT_PARAMETER)), AS2SignatureAlgorithm.SHA256WITHRSA,
                serverCertList.toArray(new Certificate[0]), serverSigningKP.getPrivate(), serverSigningKP.getPrivate());

        serverConnection.listen("/", (request, response, context) ->  {
            LOG.info("Received test message: " + request);
            context.setAttribute(AS2ServerManager.FROM, MDN_FROM);
            context.setAttribute(AS2ServerManager.SUBJECT, MDN_SUBJECT_PREFIX);
        });
    }

}
