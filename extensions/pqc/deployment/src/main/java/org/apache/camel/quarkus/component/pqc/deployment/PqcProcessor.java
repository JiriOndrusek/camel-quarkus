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
package org.apache.camel.quarkus.component.pqc.deployment;

import java.security.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.pqc.PqcRecorder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class PqcProcessor {

    private static final Logger LOG = Logger.getLogger(PqcProcessor.class);
    private static final String FEATURE = "camel-pqc";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageSecurityProviderBuildItem registerBcpqcSecurityProvider() {
        return new NativeImageSecurityProviderBuildItem("org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider");
    }

    @BuildStep
    IndexDependencyBuildItem indexBouncyCastlePQC() {
        return new IndexDependencyBuildItem("org.bouncycastle", "bcprov-jdk18on");
    }

    @BuildStep
    ReflectiveClassBuildItem registerBouncyCastlePQCClasses(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] pqcClasses = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.bouncycastle.pqc.jcajce.provider.") ||
                        n.startsWith("org.bouncycastle.pqc.jcajce.spec.") ||
                        n.startsWith("org.bouncycastle.pqc.crypto.") ||
                        n.startsWith("org.bouncycastle.jcajce.spec.KEM"))
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(pqcClasses).methods().fields().build();
    }

    @BuildStep
    void registerCryptoClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(
                        java.security.KeyPairGenerator.class,
                        java.security.Signature.class,
                        java.security.KeyFactory.class,
                        javax.crypto.KeyGenerator.class,
                        javax.crypto.SecretKey.class).methods().build());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerBouncyCastlePQCProvider(PqcRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem) {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = discoverPqcAlgorithms(provider);

        LOG.infof("Discovered %d PQC algorithms from BouncyCastlePQCProvider", transformations.size());

        recorder.registerBouncyCastlePQCProvider(transformations, shutdownContextBuildItem);
    }

    /**
     * Discovers PQC algorithms from the given provider, filtering out parameter spec variants
     * and normalizing names to match the original hardcoded list format.
     * This method is package-private to allow testing.
     */
    static List<String> discoverPqcAlgorithms(Provider provider) {
        return provider.getServices().stream()
                .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                // Filter out parameter spec variants (e.g., DILITHIUM2, ML-KEM-512, Falcon-512)
                // These are handled via ParameterSpec at runtime, not as separate base algorithms
                .filter(alg -> !alg.matches(".*[-]?\\d+.*")) // Exclude names with digits
                .filter(alg -> !alg.contains("WITH")) // Exclude composite signature schemes like SHA3-512WITHSPHINCS256
                .map(PqcProcessor::normalizeAlgorithmName) // Normalize to match original hardcoded list case
                .collect(Collectors.toList());
    }

    /**
     * Normalizes algorithm names to match the case format from the original hardcoded list.
     * This ensures backwards compatibility with existing integration tests.
     */
    private static String normalizeAlgorithmName(String algorithm) {
        // Map common PQC algorithm names to their canonical case format
        switch (algorithm.toUpperCase()) {
        case "DILITHIUM":
            return "Dilithium";
        case "FALCON":
            return "Falcon";
        case "SPHINCSPLUS":
            return "SPHINCSPlus";
        case "KYBER":
            return "Kyber";
        case "SPHINCS+":
            return "SPHINCS+";
        case "NTRU":
            return "NTRU";
        case "SABER":
            return "SABER";
        case "FRODO":
            return "FrodoKEM";
        case "ML-KEM":
            return "ML-KEM";
        default:
            // For unknown algorithms, return as-is (future-proofing)
            return algorithm;
        }
    }
}
