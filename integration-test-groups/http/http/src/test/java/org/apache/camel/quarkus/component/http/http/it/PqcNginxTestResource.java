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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.pqc.certificate.HybridCertificateFiles;
import org.apache.camel.quarkus.test.support.pqc.certificate.PQCCertificateGenerationExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Test resource that starts an nginx container with PQC hybrid certificates.
 *
 * Certificates are generated via @PQCCertificates annotation on the test class.
 * This resource only handles nginx container lifecycle.
 */
public class PqcNginxTestResource implements QuarkusTestResourceLifecycleManager {
    public static final String TRUSTSTORE_PASSWORD = "changeit";
    public static final String CERT_NAME = "nginx-hybrid-pqc";
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

            // Get certificate files from the PQC certificate generation extension
            PQCCertificateGenerationExtension extension = PQCCertificateGenerationExtension.getInstance(HttpTest.class);
            if (extension == null || extension.getCertificateFiles().isEmpty()) {
                throw new IllegalStateException(
                        "PQC certificates not found. Expected @PQCCertificates annotation to generate them on HttpTest class");
            }

            // Get the first (and only) certificate - nginx-hybrid-pqc
            HybridCertificateFiles certFiles = extension.getCertificateFiles().get(0);
            Path certFile = certFiles.getCertificatePem();
            Path keyFile = certFiles.getPrimaryKeyPem();
            Path truststoreFile = certFiles.getTruststore();
            Path certDirPath = certFile.getParent();

            if (!certFile.toFile().exists() || !keyFile.toFile().exists()) {
                throw new IllegalStateException(
                        "PQC certificate files not found at expected locations: cert=" + certFile + ", key=" + keyFile);
            }

            // Write nginx configuration
            // Certificate contains both RSA (primary) and Dilithium2/ML-DSA (alternative) keys
            // Use filenames from the certificate files
            String certFileName = certFile.getFileName().toString();
            String keyFileName = keyFile.getFileName().toString();

            String nginxConfig = String.format("""
                    server {
                        listen 4433 ssl;
                        server_name localhost;
                        ssl_certificate /certs/%s;
                        ssl_certificate_key /certs/%s;
                        ssl_protocols TLSv1.3;
                        location /test {
                            return 200 "Hybrid RSA+Dilithium(ML-DSA) certificate validated";
                            add_header Content-Type text/plain;
                        }
                    }
                    """, certFileName, keyFileName);

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

            // Return configuration properties using paths from certificate files
            Map<String, String> result = new LinkedHashMap<>();
            result.put("pqc.nginx.host", container.getHost());
            result.put("pqc.nginx.port", String.valueOf(container.getMappedPort(4433)));
            result.put("pqc.nginx.truststore.path", "file://" + truststoreFile.toAbsolutePath());
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
