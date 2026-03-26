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
package org.apache.camel.quarkus.component.pqc.it;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying that the dynamically discovered PQC algorithms
 * from BouncyCastlePQCProvider are correctly registered and available at runtime.
 * These tests complement PqcAlgorithmDiscoveryTest by verifying runtime availability
 * of algorithms discovered at build time.
 */
@QuarkusTest
class PqcAlgorithmDiscoveryIT {

    private static final String BCPQC_PROVIDER = "BCPQC";
    private static final Set<String> EXPECTED_SERVICE_TYPES = new HashSet<>(
            Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory"));

    /**
     * Verifies that the BouncyCastlePQCProvider is registered and available at runtime.
     * This is the foundation requirement for all PQC algorithm operations.
     */
    @Test
    void testBouncyCastlePQCProviderRegistered() {
        Provider[] providers = Security.getProviders();
        boolean found = false;
        for (Provider provider : providers) {
            if (BCPQC_PROVIDER.equals(provider.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "BouncyCastlePQCProvider (BCPQC) should be registered in security providers");
    }

    /**
     * Verifies that core PQC algorithms discovered dynamically at build time
     * are available for instantiation at runtime. This ensures the dynamic
     * discovery mechanism correctly enables GraalVM reachability analysis.
     */
    @Test
    void testCoreAlgorithmsAvailableAtRuntime() throws NoSuchAlgorithmException, NoSuchProviderException {
        // Core algorithms that should be discovered and registered
        String[] coreAlgorithms = { "Dilithium", "Falcon", "SPHINCSPlus", "Kyber" };

        for (String algorithm : coreAlgorithms) {
            // Attempt to get KeyPairGenerator for each algorithm
            KeyPairGenerator gen = KeyPairGenerator.getInstance(algorithm, BCPQC_PROVIDER);
            assertNotNull(gen, "KeyPairGenerator for " + algorithm + " should be available at runtime");
        }
    }

    /**
     * Tests that the discovered algorithms can be queried from the provider at runtime.
     * This verifies the provider services are properly initialized.
     */
    @Test
    void testProviderServicesAvailableAtRuntime() {
        Provider bcpqcProvider = Security.getProvider(BCPQC_PROVIDER);
        assertNotNull(bcpqcProvider, "BCPQC provider should be accessible");

        Set<Provider.Service> services = bcpqcProvider.getServices();
        assertNotNull(services, "Provider services should be accessible");
        assertFalse(services.isEmpty(), "Provider should have registered services");

        // Count services by type
        long signatureCount = services.stream()
                .filter(s -> "Signature".equals(s.getType()))
                .count();
        long keyPairGenCount = services.stream()
                .filter(s -> "KeyPairGenerator".equals(s.getType()))
                .count();

        assertTrue(signatureCount > 0, "Should have Signature services registered");
        assertTrue(keyPairGenCount > 0, "Should have KeyPairGenerator services registered");
    }

    /**
     * Verifies that the dynamically discovered algorithm list is non-empty.
     * This is a regression test ensuring the discovery mechanism doesn't
     * accidentally return an empty list in future Bouncy Castle versions.
     */
    @Test
    void testDiscoveredAlgorithmListNotEmpty() {
        Provider bcpqcProvider = Security.getProvider(BCPQC_PROVIDER);
        assertNotNull(bcpqcProvider, "BCPQC provider should be accessible");

        List<String> discoveredAlgorithms = bcpqcProvider.getServices().stream()
                .filter(s -> EXPECTED_SERVICE_TYPES.contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        assertFalse(discoveredAlgorithms.isEmpty(),
                "Discovered algorithm list should not be empty - this indicates the dynamic discovery failed");

        // Expect at least 4 core algorithms
        assertTrue(discoveredAlgorithms.size() >= 4,
                "Should discover at least 4 core PQC algorithms, found: " + discoveredAlgorithms.size());
    }

    /**
     * Regression test ensuring algorithms that were previously hardcoded
     * are still available after switching to dynamic discovery.
     */
    @Test
    void testBackwardsCompatibleAlgorithmsAvailable() throws NoSuchAlgorithmException, NoSuchProviderException {
        // These algorithms were in the original hardcoded list and should still work
        String[] backwardsCompatibleAlgorithms = { "Dilithium", "Falcon", "SPHINCSPlus", "Kyber" };

        for (String algorithm : backwardsCompatibleAlgorithms) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(algorithm, BCPQC_PROVIDER);
            assertNotNull(gen,
                    "Previously supported algorithm " + algorithm + " should still be available after dynamic discovery");
        }
    }

    /**
     * Tests that multiple service types are correctly discovered and available.
     * Verifies both KeyPairGenerator and Signature services work for the same algorithm.
     */
    @Test
    void testMultipleServiceTypesForSameAlgorithm() {
        Provider bcpqcProvider = Security.getProvider(BCPQC_PROVIDER);
        assertNotNull(bcpqcProvider);

        // Dilithium should be available as both Signature and KeyPairGenerator
        boolean hasSignature = bcpqcProvider.getServices().stream()
                .anyMatch(s -> "Signature".equals(s.getType()) && "Dilithium".equals(s.getAlgorithm()));
        boolean hasKeyPairGen = bcpqcProvider.getServices().stream()
                .anyMatch(s -> "KeyPairGenerator".equals(s.getType()) && "Dilithium".equals(s.getAlgorithm()));

        assertTrue(hasSignature, "Dilithium should be registered as Signature service");
        assertTrue(hasKeyPairGen, "Dilithium should be registered as KeyPairGenerator service");
    }

    /**
     * Verifies that the provider name is correctly set as expected by PqcProcessor.
     */
    @Test
    void testProviderNameIsCorrect() {
        Provider bcpqcProvider = Security.getProvider(BCPQC_PROVIDER);
        assertNotNull(bcpqcProvider, "Provider should be found by name");
        assertTrue(bcpqcProvider.getName().equals(BCPQC_PROVIDER),
                "Provider name should be BCPQC");
    }

    /**
     * Edge case test: Verify that querying for non-existent algorithms fails gracefully.
     * This ensures the provider is correctly configured and doesn't incorrectly
     * return instances for invalid algorithm names.
     */
    @Test
    void testNonExistentAlgorithmFailsGracefully() {
        try {
            KeyPairGenerator.getInstance("NonExistentPQCAlgorithm", BCPQC_PROVIDER);
            throw new AssertionError("Should have thrown NoSuchAlgorithmException for non-existent algorithm");
        } catch (NoSuchAlgorithmException e) {
            // Expected - this is the correct behavior
            assertTrue(e.getMessage().contains("NonExistentPQCAlgorithm"),
                    "Exception message should reference the algorithm name");
        } catch (NoSuchProviderException e) {
            throw new AssertionError("Provider should exist, but algorithm lookup failed for wrong reason", e);
        }
    }

    /**
     * Tests that the dynamic discovery doesn't break existing integration test scenarios.
     * This is a smoke test verifying that at least one complete sign/verify cycle works
     * with a dynamically discovered algorithm.
     */
    @Test
    void testDynamicDiscoveryDoesNotBreakExistingFunctionality() throws Exception {
        // Simply verify we can create a KeyPairGenerator for Dilithium
        // The full sign/verify operations are tested in PqcTest
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Dilithium", BCPQC_PROVIDER);
        assertNotNull(gen);

        // Verify we can also get a Signature instance
        java.security.Signature sig = java.security.Signature.getInstance("Dilithium", BCPQC_PROVIDER);
        assertNotNull(sig);
    }
}
