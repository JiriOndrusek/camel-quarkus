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
package org.apache.camel.quarkus.test.support.pqc.certificate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit5 extension that generates PQC certificates before tests run.
 * Invoked via {@link PQCCertificates} annotation.
 *
 * <p>
 * Extension behavior:
 * <ul>
 * <li>Scans test class for {@link PQCCertificates} annotation</li>
 * <li>Creates base directory if it doesn't exist</li>
 * <li>For each {@link PQCCertificate}:
 * <ul>
 * <li>Checks if certificate files already exist (unless replaceIfExists=true)</li>
 * <li>Handles Docker host resolution (if docker=true)</li>
 * <li>Invokes {@link HybridCertificateGenerator} to create certificate</li>
 * <li>Stores generated {@link HybridCertificateFiles} in global extension store</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>
 * Certificates are stored in JUnit5's global extension store, making them accessible
 * across test class hierarchies (e.g., HttpTest -> HttpIT for native tests).
 */
public class PQCCertificateGenerationExtension implements BeforeAllCallback {

    private static final Logger LOG = Logger.getLogger(PQCCertificateGenerationExtension.class);

    /**
     * Global namespace for storing certificate files in the extension store.
     * This allows access from QuarkusTestResourceLifecycleManager and works across JVM/native test inheritance.
     */
    private static final ExtensionContext.Namespace CERTIFICATE_NAMESPACE = ExtensionContext.Namespace
            .create(PQCCertificateGenerationExtension.class, "certificates");

//    /**
//     * Key for the certificate map in the extension store.
//     */
//    private static final String CERTIFICATE_MAP_KEY = "certificateMap";
//
//    /**
//     * Static registry of extension instances by test class name.
//     * Kept for backwards compatibility, but extension store is preferred.
//     */
//    private static final Map<String, PQCCertificateGenerationExtension> REGISTRY = new ConcurrentHashMap<>();

    /**
     * Retrieves the extension instance from the extension context.
     *
     * @param  extensionContext JUnit5 extension context
     * @return                  The PQCCertificateGenerationExtension instance
     */
    public static PQCCertificateGenerationExtension getInstance(ExtensionContext extensionContext) {
        return extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(PQCCertificateGenerationExtension.class, PQCCertificateGenerationExtension.class);
    }

    /**
     * Finds certificate files by name from the global extension store.
     * Works reliably across JVM and native test modes, and with test class inheritance.
     *
     * This is the preferred method for QuarkusTestResourceLifecycleManager implementations
     * that don't have direct access to the test class or ExtensionContext.
     *
     * @param  certificateName The certificate name (e.g., "nginx-hybrid-pqc")
     * @return                 HybridCertificateFiles if found, null otherwise
     */
    public static HybridCertificateFiles findCertificateByName(String certificateName) {
        // Access the global store via the static context holder
        // This is set during beforeAll() and persists across test hierarchy
        Map<String, HybridCertificateFiles> map = GLOBAL_CERTIFICATE_MAP;
        HybridCertificateFiles files =  map != null ? map.get(certificateName) : null;

        if (files != null) {
            LOG.debugf("Found certificate '%s' in global extension store", certificateName);
            return files;
        }

        LOG.warnf("Certificate '%s' not found in extension store or registry", certificateName);
        return null;
    }

    /**
     * Stores a certificate in the global extension store by name.
     *
     * @param certificateName The certificate name
     * @param files           The certificate files to store
     * @param context         The extension context
     */
    private static void storeCertificateInGlobalStore(String certificateName, HybridCertificateFiles files,
            ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(CERTIFICATE_NAMESPACE);
        @SuppressWarnings("unchecked")
        Map<String, HybridCertificateFiles> map = store.getOrComputeIfAbsent(
                CERTIFICATE_MAP_KEY,
                key -> new ConcurrentHashMap<String, HybridCertificateFiles>(),
                Map.class);
        map.put(certificateName, files);
        LOG.infof("✅  Stored certificate '%s' in global extension store", certificateName);
    }

    /**
     * Static reference to the certificate map for access without ExtensionContext.
     * This is populated during test initialization and persists throughout the test execution.
     */
    private static volatile Map<String, HybridCertificateFiles> GLOBAL_CERTIFICATE_MAP = new ConcurrentHashMap<>();

    /**
     * List of generated certificate files for this test class.
     */
    List<HybridCertificateFiles> certificateFiles = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        var maybe = AnnotationUtils.findAnnotation(extensionContext.getRequiredTestClass(), PQCCertificates.class);
        if (maybe.isEmpty()) {
            return;
        }
        var annotation = maybe.get();

        // Resolve Docker host if needed (for external Docker hosts like Docker Desktop on Mac/Windows)
        Optional<String> dockerHost = Optional.empty();
        if (annotation.docker()) {
            dockerHost = resolveDockerHost(extensionContext);
        }

        // Create base directory
        File baseDir = new File(annotation.baseDir());
        baseDir.mkdirs();
        Path basePath = baseDir.toPath();

        LOG.infof("🔧  Generating PQC certificates in: %s", basePath.toAbsolutePath());

        // Generate each certificate
        HybridCertificateGenerator generator = new HybridCertificateGenerator();

        for (PQCCertificate certificate : annotation.certificates()) {
            // Determine CN and SANs (with Docker host override if needed)
            String cn = dockerHost.orElse(certificate.cn());
            List<String> sans = new ArrayList<>(Arrays.asList(certificate.subjectAlternativeNames()));

            // Add Docker host as IP SAN if Docker mode enabled
            dockerHost.ifPresent(host -> sans.add("IP:" + host));

            // Check if certificate already exists (skip if not replaceIfExists)
            Path certPath = basePath.resolve(certificate.name() + "-cert.pem");
            HybridCertificateFiles files;

            if (Files.exists(certPath) && !annotation.replaceIfExists()) {
                LOG.infof("⏩  Certificate already exists, skipping generation: %s", certPath);
                // Load existing certificate paths into stores/registry
                Path primaryKeyPath = basePath.resolve(certificate.name() + "-key.pem");
                Path pqcKeyPath = basePath.resolve(certificate.name() + "-pqc-key.pem");
                Path truststorePath = basePath.resolve(certificate.name() + "-truststore.p12");
                Path keystorePath = basePath.resolve(certificate.name() + "-keystore.p12");

                files = new HybridCertificateFiles(
                        certPath,
                        primaryKeyPath,
                        pqcKeyPath,
                        truststorePath,
                        keystorePath);
            } else {
                // Generate certificate based on hybrid mode
                Duration validity = Duration.ofDays(certificate.validity());

                if (certificate.hybridMode() == HybridMode.CHIMERA) {
                    LOG.infof("🔨  Generating Chimera certificate '%s': %s + %s (CN=%s)",
                            certificate.name(),
                            certificate.primaryAlgorithm(),
                            certificate.pqcAlgorithm().getAlgorithmName(),
                            cn);

                    files = generator.generateChimeraCertificate(
                            certificate.name(),
                            certificate.primaryAlgorithm(),
                            certificate.pqcAlgorithm(),
                            cn,
                            sans,
                            validity,
                            basePath,
                            certificate.password());

                } else if (certificate.hybridMode() == HybridMode.PQC_ONLY) {
                    LOG.infof("🔨  Generating pure PQC certificate '%s': %s (CN=%s)",
                            certificate.name(),
                            certificate.pqcAlgorithm().getAlgorithmName(),
                            cn);

                    files = generator.generatePurePQCCertificate(
                            certificate.name(),
                            certificate.pqcAlgorithm(),
                            cn,
                            sans,
                            validity,
                            basePath,
                            certificate.password());

                } else {
                    throw new IllegalArgumentException("Unsupported hybrid mode: " + certificate.hybridMode());
                }
            }

            certificateFiles.add(files);

            // Store in both the extension store and the static map
            storeCertificateInGlobalStore(certificate.name(), files, extensionContext);
            GLOBAL_CERTIFICATE_MAP.put(certificate.name(), files);
        }

        if (certificateFiles.size() > 0) {
            LOG.infof("✅  PQC certificate generation complete. Generated %d certificate(s).", certificateFiles.size());
        }
    }

    /**
     * Resolves the Docker host IP address for external Docker hosts.
     * Used when docker=true to override CN and SANs with the actual Docker host.
     *
     * @param  extensionContext JUnit5 extension context
     * @return                  Optional Docker host IP (empty if localhost/127.0.0.1)
     */
    private Optional<String> resolveDockerHost(ExtensionContext extensionContext) {
        String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
        if (!dockerHost.equals("localhost") && !dockerHost.equals("127.0.0.1")) {
            LOG.infof("Detected external Docker host: %s", dockerHost);
            return Optional.of(dockerHost);
        }
        return Optional.empty();
    }
}
