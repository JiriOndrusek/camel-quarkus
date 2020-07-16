package org.apache.camel.quarkus.component.as2.it.util;

import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.AS2SignedDataGenerator;
import org.apache.camel.component.as2.api.util.SigningUtils;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class As2ServerHelper {

    private static AS2SignedDataGenerator gen;

    private static KeyPair issueKP;
    private static X509Certificate issueCert;

    private static KeyPair signingKP;
    private static X509Certificate signingCert;
    private static List<X509Certificate> certList;

    private static KeyPair decryptingKP;
    
    public static void setupSigningGenerator() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        setupKeysAndCertificates();

        // Create and populate certificate store.
        JcaCertStore certs = new JcaCertStore(certList);

        // Create capabilities vector
        SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
        capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
        capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
        capabilities.addCapability(SMIMECapability.dES_CBC);

        // Create signing attributes
        ASN1EncodableVector attributes = new ASN1EncodableVector();
        attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(new IssuerAndSerialNumber(new X500Name(signingCert.getIssuerDN().getName()), signingCert.getSerialNumber())));
        attributes.add(new SMIMECapabilitiesAttribute(capabilities));

        gen = SigningUtils.createSigningGenerator(AS2SignatureAlgorithm.SHA256WITHRSA, certList.toArray(new X509Certificate[0]), signingKP.getPrivate());
        gen.addCertificates(certs);

    }


    private static void setupKeysAndCertificates() throws Exception {
        //
        // set up our certificates
        //
        KeyPairGenerator kpg  = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        issueKP = kpg.generateKeyPair();
        issueCert = Utils.makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        signingKP = kpg.generateKeyPair();
        signingCert = Utils.makeCertificate(
                signingKP, signingDN, issueKP, issueDN);

        certList = new ArrayList<>();

        certList.add(signingCert);
        certList.add(issueCert);

        decryptingKP = signingKP;

    }

}
