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
import java.io.OutputStream;
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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class PqcNginxTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String BCPQC_PROVIDER = "BCPQC";
    private static final String CERT_DIR = "target/certs/pqc-nginx";
    private static final String TRUSTSTORE_PASSWORD = "changeit";
    private static final String CONTAINER_IMAGE_PROPERTY = "openquantumsafe-nginx.container.image";

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

            // Generate Dilithium2 keypair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium2", BCPQC_PROVIDER);
            KeyPair keyPair = kpg.generateKeyPair();

            // Create self-signed certificate
            Instant now = Instant.now();
            Date notBefore = Date.from(now.minus(1, ChronoUnit.HOURS)); // 1 hour in the past to avoid clock skew
            Date notAfter = Date.from(now.plus(30, ChronoUnit.DAYS));

            X500Name subject = new X500Name("CN=nginx-pqc");
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    serialNumber,
                    notBefore,
                    notAfter,
                    subject,
                    keyPair.getPublic());

            // Create a custom ContentSigner using Signature API directly
            final Signature signature = Signature.getInstance("Dilithium2", BCPQC_PROVIDER);
            signature.initSign(keyPair.getPrivate());
            final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("Dilithium2");

            ContentSigner signer = new ContentSigner() {
                private OutputStream stream = new OutputStream() {
                    @Override
                    public void write(int b) {
                        try {
                            signature.update((byte) b);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void write(byte[] bytes, int off, int len) {
                        try {
                            signature.update(bytes, off, len);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                @Override
                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return sigAlgId;
                }

                @Override
                public OutputStream getOutputStream() {
                    return stream;
                }

                @Override
                public byte[] getSignature() {
                    try {
                        return signature.sign();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            X509Certificate certificate = new JcaX509CertificateConverter()
                    .getCertificate(certBuilder.build(signer));

            // Export private key to PEM format
            File keyFile = certDirPath.resolve("key.pem").toFile();
            try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(keyFile)))) {
                pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
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
            String nginxConfig = """
                    server {
                        listen 4433 ssl;
                        server_name localhost;
                        ssl_certificate /certs/cert.pem;
                        ssl_certificate_key /certs/key.pem;
                        ssl_protocols TLSv1.3;
                        location /test {
                            return 200 "PQC TLS connection successful";
                            add_header Content-Type text/plain;
                        }
                    }
                    """;

            File nginxConfigFile = certDirPath.resolve("default.conf").toFile();
            Files.writeString(nginxConfigFile.toPath(), nginxConfig);

            // Start nginx container
            String imageName = ConfigProvider.getConfig().getValue(CONTAINER_IMAGE_PROPERTY, String.class);
            DockerImageName dockerImageName = DockerImageName.parse(imageName);

            container = new GenericContainer<>(dockerImageName)
                    .withExposedPorts(4433)
                    .withFileSystemBind(certDirPath.toAbsolutePath().toString(), "/certs", BindMode.READ_ONLY)
                    .withFileSystemBind(nginxConfigFile.getAbsolutePath(), "/opt/nginx/nginx-conf/servers/default.conf",
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
