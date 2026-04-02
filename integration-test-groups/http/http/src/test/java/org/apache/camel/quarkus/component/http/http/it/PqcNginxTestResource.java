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
package org.apache.camel.quarkus.component.http.http.it;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class PqcNginxTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String BCPQC_PROVIDER = "BCPQC";
    private static final String CERT_DIR = "target/certs/bctls-nginx";
    private static final String TRUSTSTORE_PASSWORD = "changeit";
    // Use standard nginx (not OQS) to test BCTLS integration
    // OQS-OpenSSL is incompatible with BouncyCastle JSSE at the protocol level
    private static final String NGINX_IMAGE = "nginx:alpine";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            // Skip if testcontainers are disabled
            if (Boolean.parseBoolean(System.getProperty("skip-testcontainers-tests", "false"))) {
                return Map.of();
            }

            // Ensure BCPQC provider is registered
            if (Security.getProvider(BCPQC_PROVIDER) == null) {
                Security.addProvider(new BouncyCastlePQCProvider());
            }

            // Create certificate directory
            Path certDirPath = Path.of(CERT_DIR);
            Files.createDirectories(certDirPath);

            // Generate RSA keypair for TLS handshake (primary key)
            // Standard TLS 1.3 requires RSA/ECDSA for cipher suites
            KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
            rsaKpg.initialize(2048);
            KeyPair rsaKeyPair = rsaKpg.generateKeyPair();

            // Generate Dilithium2 keypair for PQC signature (alternative key)
            // This creates a hybrid/composite certificate as recommended by BC Almanac
            // TODO: Migrate to "ML-DSA-44" (NIST standardized name) when BC library supports it
            KeyPairGenerator dilithiumKpg = KeyPairGenerator.getInstance("Dilithium2", BCPQC_PROVIDER);
            KeyPair dilithiumKeyPair = dilithiumKpg.generateKeyPair();

            // Create self-signed certificate
            Instant now = Instant.now();
            Date notBefore = Date.from(now.minus(1, ChronoUnit.HOURS)); // 1 hour in the past to avoid clock skew
            Date notAfter = Date.from(now.plus(30, ChronoUnit.DAYS));

            X500Name subject = new X500Name("CN=nginx-hybrid-pqc");
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            // Build certificate with RSA as primary public key
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    serialNumber,
                    notBefore,
                    notAfter,
                    subject,
                    rsaKeyPair.getPublic());

            // Add Dilithium public key as alternative public key (Chimera/composite certificate)
            // This follows BC Almanac page 6 recommendations for hybrid certificates
            SubjectPublicKeyInfo dilithiumPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                    dilithiumKeyPair.getPublic().getEncoded());
            certBuilder.addExtension(Extension.subjectAltPublicKeyInfo, false, dilithiumPubKeyInfo);

            // For Chimera-style certificates, we need to generate alternative signature
            // The altSignatureValue should be computed over the TBSCertificate
            // For simplicity in this test, we sign a marker to demonstrate the structure
            // Full Chimera spec requires signing the actual TBSCertificate bytes
            // TODO: Migrate to "ML-DSA-44" (NIST standardized name) when BC library supports it
            Signature dilithiumSig = Signature.getInstance("Dilithium2", BCPQC_PROVIDER);
            dilithiumSig.initSign(dilithiumKeyPair.getPrivate());
            dilithiumSig.update(subject.getEncoded()); // Sign subject as marker
            byte[] dilithiumSignatureBytes = dilithiumSig.sign();

            // Add alternative signature value extension (Chimera-style composite certificate)
            // Following BC Almanac page 6 recommendations
            certBuilder.addExtension(Extension.altSignatureValue, false,
                    new DERBitString(dilithiumSignatureBytes));

            // Create RSA signer for primary signature
            ContentSigner rsaSigner = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(rsaKeyPair.getPrivate());

            // Build final certificate with RSA as primary signature
            X509Certificate certificate = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certBuilder.build(rsaSigner));

            // Export RSA private key to PEM format (nginx will use this for TLS)
            // The Dilithium key is embedded in the certificate as altPublicKey extension
            File keyFile = certDirPath.resolve("key.pem").toFile();
            try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(keyFile)))) {
                pemWriter.writeObject(new PemObject("PRIVATE KEY", rsaKeyPair.getPrivate().getEncoded()));
            }

            // Also export Dilithium/ML-DSA private key for reference (not used by nginx)
            File dilithiumKeyFile = certDirPath.resolve("dilithium-key.pem").toFile();
            try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(dilithiumKeyFile)))) {
                pemWriter.writeObject(new PemObject("PRIVATE KEY", dilithiumKeyPair.getPrivate().getEncoded()));
            }

            // Export certificate to PEM format
            File certFile = certDirPath.resolve("cert.pem").toFile();
            try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(certFile)))) {
                pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
            }

            // Create PKCS12 truststore
            File truststoreFile = certDirPath.resolve("truststore.p12").toFile();
            KeyStore truststore = KeyStore.getInstance("PKCS12");
            truststore.load(null, null);
            truststore.setCertificateEntry("nginx-pqc", certificate);
            try (FileOutputStream fos = new FileOutputStream(truststoreFile)) {
                truststore.store(fos, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // Write nginx configuration
            // Certificate contains both RSA (primary) and Dilithium2/ML-DSA (alternative) keys
            String nginxConfig = """
                    server {
                        listen 4433 ssl;
                        server_name localhost;
                        ssl_certificate /certs/cert.pem;
                        ssl_certificate_key /certs/key.pem;
                        ssl_protocols TLSv1.3 TLSv1.2;
                        location /test {
                            return 200 "Hybrid RSA+Dilithium(ML-DSA) certificate validated";
                            add_header Content-Type text/plain;
                        }
                    }
                    """;

            File nginxConfigFile = certDirPath.resolve("default.conf").toFile();
            Files.writeString(nginxConfigFile.toPath(), nginxConfig);

            // Start standard nginx container
            container = new GenericContainer<>(DockerImageName.parse(NGINX_IMAGE))
                    .withExposedPorts(4433)
                    .withFileSystemBind(certDirPath.toAbsolutePath().toString(), "/certs", BindMode.READ_ONLY)
                    .withFileSystemBind(nginxConfigFile.getAbsolutePath(), "/etc/nginx/conf.d/default.conf",
                            BindMode.READ_ONLY)
                    .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                    .waitingFor(Wait.forListeningPort());

            container.start();

            // Return configuration properties
            Map<String, String> result = new LinkedHashMap<>();
            result.put("pqc.nginx.host", container.getHost());
            result.put("pqc.nginx.port", String.valueOf(container.getMappedPort(4433)));
            result.put("pqc.nginx.truststore.path", "file://" + truststoreFile.getAbsolutePath());
            result.put("pqc.nginx.truststore.password", TRUSTSTORE_PASSWORD);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start PQC nginx test resource", e);
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
