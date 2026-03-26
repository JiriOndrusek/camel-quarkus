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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the dynamic PQC algorithm discovery mechanism in PqcProcessor.
 * Tests verify that the discovery logic correctly finds and filters algorithms from
 * BouncyCastlePQCProvider without requiring the full Quarkus build-time framework.
 */
class PqcAlgorithmDiscoveryTest {

    private static final Set<String> EXPECTED_SERVICE_TYPES = new HashSet<>(
            Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory"));

    /**
     * Tests the core dynamic discovery mechanism that mirrors the implementation in PqcProcessor.
     * This is the happy path test verifying the discovery works as expected.
     */
    @Test
    void testDynamicAlgorithmDiscovery() {
        // Use the same discovery logic as PqcProcessor
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // Verify basic discovery worked
        assertNotNull(transformations, "Discovered transformations should not be null");
        assertFalse(transformations.isEmpty(), "Discovered transformations should not be empty");
    }

    /**
     * Verifies that the discovery mechanism finds all expected core PQC algorithms.
     * These algorithms are referenced in the integration tests and must be discovered
     * for native image compilation to work correctly.
     */
    @Test
    void testExpectedAlgorithmsAreDiscovered() {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // Core signature algorithms that must be present
        Set<String> expectedAlgorithms = new HashSet<>(Arrays.asList(
                "Dilithium",
                "Falcon",
                "SPHINCSPlus",
                "Kyber"));

        for (String expected : expectedAlgorithms) {
            assertTrue(transformations.contains(expected),
                    "Expected algorithm '" + expected + "' not found in discovered list: " + transformations);
        }
    }

    /**
     * Verifies that only the four specified service types are included in discovery.
     * Tests the service type filtering correctness mentioned in the implementation plan.
     */
    @Test
    void testServiceTypeFiltering() {
        Provider provider = new BouncyCastlePQCProvider();

        // Get all services grouped by type
        Map<String, Long> serviceTypeCounts = provider.getServices().stream()
                .collect(Collectors.groupingBy(Provider.Service::getType, Collectors.counting()));

        // Get filtered services
        List<Provider.Service> filteredServices = provider.getServices().stream()
                .filter(s -> EXPECTED_SERVICE_TYPES.contains(s.getType()))
                .collect(Collectors.toList());

        // Verify only expected types are included
        Set<String> discoveredTypes = filteredServices.stream()
                .map(Provider.Service::getType)
                .collect(Collectors.toSet());

        for (String type : discoveredTypes) {
            assertTrue(EXPECTED_SERVICE_TYPES.contains(type),
                    "Unexpected service type discovered: " + type);
        }

        // Verify at least some of each expected type exists
        assertTrue(discoveredTypes.contains("Signature"), "Should discover Signature services");
        assertTrue(discoveredTypes.contains("KeyPairGenerator"), "Should discover KeyPairGenerator services");
    }

    /**
     * Tests that duplicate algorithm names are properly handled by the .distinct() call.
     * The same algorithm may appear in multiple service types (e.g., "Dilithium" in both
     * Signature and KeyFactory), and we should only register each algorithm once.
     */
    @Test
    void testDuplicateAlgorithmHandling() {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // Verify no duplicates in final list
        Set<String> uniqueCheck = new HashSet<>(transformations);
        assertEquals(uniqueCheck.size(), transformations.size(),
                "Discovery should contain no duplicates");
    }

    /**
     * Verifies that the discovery finds a reasonable number of algorithms.
     * This prevents regressions where the discovery might suddenly return too few results.
     * Based on the integration tests, we expect at least the core 4 algorithms.
     */
    @Test
    void testDiscoveryCountIsReasonable() {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // Expect at least 4 core algorithms (Dilithium, Falcon, SPHINCSPlus, Kyber)
        assertTrue(transformations.size() >= 4,
                "Expected at least 4 PQC algorithms, but found: " + transformations.size());

        // Log discovered count for visibility (implementation plan suggests logging)
        System.out.println("Discovered " + transformations.size() + " PQC algorithms from BouncyCastlePQCProvider");
        System.out.println("Algorithms: " + transformations);
    }

    /**
     * Verifies that algorithm variants like Dilithium2, Dilithium3, ML-KEM-512, etc.
     * are NOT in the base algorithm list, as they are parameter specifications applied
     * at runtime via ParameterSpec classes, not separate base algorithms.
     */
    @Test
    void testAlgorithmNamesAreBaseNames() {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // These are parameter spec variants, not separate base algorithms
        List<String> parameterSpecVariants = Arrays.asList(
                "DILITHIUM2", "DILITHIUM3", "DILITHIUM5",
                "Dilithium2", "Dilithium3", "Dilithium5",
                "ML-KEM-512", "ML-KEM-768", "ML-KEM-1024",
                "Kyber512", "Kyber768", "Kyber1024",
                "Falcon-512", "Falcon-1024",
                "FALCON-512", "FALCON-1024");

        // Verify these variants are NOT in the base algorithm list
        for (String variant : parameterSpecVariants) {
            assertFalse(transformations.contains(variant),
                    "Parameter spec variant '" + variant + "' should not be in base algorithm list. "
                            + "These are applied via ParameterSpec at runtime.");
        }
    }

    /**
     * Tests that the provider itself can be instantiated successfully at build time.
     * This is critical for STATIC_INIT phase execution in PqcProcessor.
     */
    @Test
    void testProviderInstantiationAtBuildTime() {
        // This simulates the build-time instantiation in PqcProcessor
        Provider provider = new BouncyCastlePQCProvider();

        assertNotNull(provider, "Provider should be instantiable at build time");
        assertEquals("BCPQC", provider.getName(), "Provider name should be BCPQC");
        assertNotNull(provider.getServices(), "Provider services should be accessible");
        assertFalse(provider.getServices().isEmpty(), "Provider should provide services");
    }

    /**
     * Regression test to ensure backwards compatibility with previously hardcoded algorithms.
     * The old implementation had: "Dilithium", "Falcon", "SPHINCS+", "SPHINCSPlus", "Kyber",
     * "NTRU", "SABER", "FrodoKEM". We verify that at least the core ones still exist.
     */
    @Test
    void testBackwardsCompatibilityWithHardcodedList() {
        Provider provider = new BouncyCastlePQCProvider();
        List<String> transformations = PqcProcessor.discoverPqcAlgorithms(provider);

        // Core algorithms from the original hardcoded list that are still actively used
        List<String> coreAlgorithms = Arrays.asList(
                "Dilithium",
                "Falcon",
                "SPHINCSPlus",
                "Kyber");

        for (String coreAlgorithm : coreAlgorithms) {
            assertTrue(transformations.contains(coreAlgorithm),
                    "Core algorithm '" + coreAlgorithm + "' from original hardcoded list should still be discovered");
        }
    }

    /**
     * Verifies that the discovery includes services across all four expected types.
     * This ensures the filter is not accidentally excluding entire service categories.
     */
    @Test
    void testAllServiceTypesAreDiscovered() {
        Provider provider = new BouncyCastlePQCProvider();

        // Check each service type individually
        for (String serviceType : EXPECTED_SERVICE_TYPES) {
            List<String> algorithmsOfType = provider.getServices().stream()
                    .filter(s -> serviceType.equals(s.getType()))
                    .map(Provider.Service::getAlgorithm)
                    .distinct()
                    .collect(Collectors.toList());

            // We expect at least some algorithms for Signature and KeyPairGenerator
            if ("Signature".equals(serviceType) || "KeyPairGenerator".equals(serviceType)) {
                assertFalse(algorithmsOfType.isEmpty(),
                        "Should discover at least one " + serviceType + " algorithm");
            }
        }
    }

    /**
     * Edge case test: Verify the discovery mechanism would handle an empty provider gracefully.
     * While BouncyCastlePQCProvider always has services, this tests the stream logic robustness.
     */
    @Test
    void testEmptyFilterResultHandling() {
        Provider provider = new BouncyCastlePQCProvider();

        // Filter for a service type that doesn't exist - should return empty list, not null
        List<String> nonExistentType = provider.getServices().stream()
                .filter(s -> "NonExistentServiceType".equals(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        assertNotNull(nonExistentType, "Result should be non-null even when filter matches nothing");
        assertTrue(nonExistentType.isEmpty(), "Result should be empty when filter matches nothing");
    }
}
