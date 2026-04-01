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
package org.apache.camel.quarkus.test.support.certificate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * JUnit 5 extension for generating Post-Quantum Cryptography keypairs before test execution.
 * Generates keypairs based on @PQCKeyPairs annotation and serializes them to disk.
 */
public class PQCKeyPairGenerationExtension implements BeforeAllCallback {
    private static final Logger LOGGER = Logger.getLogger(PQCKeyPairGenerationExtension.class);
    private static final String BCPQC_PROVIDER = "BCPQC";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        var maybe = AnnotationUtils.findAnnotation(extensionContext.getRequiredTestClass(), PQCKeyPairs.class);
        if (maybe.isEmpty()) {
            return;
        }
        var annotation = maybe.get();

        // Ensure BCPQC provider is registered
        if (Security.getProvider(BCPQC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
            LOGGER.debugf("Registered %s provider", BCPQC_PROVIDER);
        }

        // Create base directory if it doesn't exist
        String baseDir = annotation.baseDir();
        File baseDirFile = new File(baseDir);
        if (!baseDirFile.exists()) {
            if (!baseDirFile.mkdirs()) {
                throw new IOException("Failed to create directory: " + baseDir);
            }
        }

        // Generate each keypair
        for (PQCKeyPair keyPairConfig : annotation.keyPairs()) {
            generateKeyPair(keyPairConfig, baseDir, annotation.replaceIfExists());
        }
    }

    private void generateKeyPair(PQCKeyPair keyPairConfig, String baseDir, boolean replaceIfExists) throws Exception {
        String name = keyPairConfig.name();
        PQCAlgorithm algorithm = keyPairConfig.algorithm();

        String publicKeyPath = Paths.get(baseDir, name + "-public.key").toString();
        String privateKeyPath = Paths.get(baseDir, name + "-private.key").toString();

        // Check if files already exist
        if (!replaceIfExists && Files.exists(Paths.get(publicKeyPath)) && Files.exists(Paths.get(privateKeyPath))) {
            LOGGER.debugf("Skipping keypair generation for '%s' - files already exist", name);
            return;
        }

        LOGGER.infof("Generating %s keypair: %s", algorithm.getAlgorithmName(), name);

        // Generate keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm.getAlgorithmName(), BCPQC_PROVIDER);
        KeyPair keyPair = kpg.generateKeyPair();

        // Serialize public key (X.509 format)
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
        Files.write(Paths.get(publicKeyPath), publicKeySpec.getEncoded());

        // Serialize private key (PKCS#8 format)
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
        Files.write(Paths.get(privateKeyPath), privateKeySpec.getEncoded());

        LOGGER.infof("Generated keypair files: %s, %s", publicKeyPath, privateKeyPath);
    }
}
