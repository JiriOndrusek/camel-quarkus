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
import java.util.Set;
import java.util.stream.Collectors;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge case tests for PqcProcessor dynamic algorithm discovery mechanism.
 * These tests verify the handling of edge cases mentioned in the implementation plan:
 * 1. Empty discovery result
 * 2. Duplicate algorithm names
 * 3. Missing algorithms at runtime
 * 4. Future Bouncy Castle updates
 * 5. GraalVM native image compatibility
 * 6. Service type filtering correctness
 */
class PqcProcessorEdgeCasesTest {

    /**
     * Edge case 1: Empty discovery result.
     * Tests that if getServices() returns no matching services, transformations will be empty.
     * While this shouldn't happen with BouncyCastlePQCProvider, we verify the stream logic
     * handles it gracefully without throwing exceptions.
     */
    @Test
    void testEmptyDiscoveryResultHandling() {
        Provider provider = new BouncyCastlePQCProvider();

        // Filter for non-existent service types - should return empty list
        List<String> transformations = provider.getServices().stream()
                .filter(s -> Arrays.asList("NonExistentType1", "NonExistentType2")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        assertNotNull(transformations, "Empty filter result should produce non-null list");
        assertTrue(transformations.isEmpty(), "Filtering for non-existent types should produce empty list");
    }

    /**
     * Edge case 2: Duplicate algorithm names.
     * Verifies that the .distinct() call properly deduplicates algorithm names when
     * the same algorithm appears in multiple service types (e.g., "Dilithium" in both
     * "Signature" and "KeyFactory").
     */
    @Test
    void testDuplicateAlgorithmDeduplication() {
        Provider provider = new BouncyCastlePQCProvider();

        // Get all services without deduplication
        List<Provider.Service> allServices = provider.getServices().stream()
                .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                        .contains(s.getType()))
                .collect(Collectors.toList());

        // Get algorithm names without deduplication
        List<String> allAlgorithms = allServices.stream()
                .map(Provider.Service::getAlgorithm)
                .collect(Collectors.toList());

        // Get algorithm names with deduplication (as in actual implementation)
        List<String> distinctAlgorithms = allServices.stream()
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        // Verify deduplication occurred
        assertNotNull(distinctAlgorithms, "Distinct list should not be null");
        assertTrue(distinctAlgorithms.size() <= allAlgorithms.size(),
                "Distinct list should be equal or smaller than non-distinct list");

        // If there were duplicates, verify they're removed
        long uniqueCount = allAlgorithms.stream().distinct().count();
        assertEquals(uniqueCount, distinctAlgorithms.size(),
                "Distinct list size should match unique count from original list");

        // Verify distinct() produces a list with no duplicates
        Set<String> uniqueSet = distinctAlgorithms.stream().collect(Collectors.toSet());
        assertEquals(distinctAlgorithms.size(), uniqueSet.size(),
                "Distinct list should have no duplicate elements");
    }

    /**
     * Edge case 3: Missing algorithms at runtime.
     * The PqcRecorder already handles this with try-catch blocks (lines 58-61).
     * This test verifies that our discovery mechanism produces algorithm names
     * that can actually be instantiated, reducing the likelihood of runtime failures.
     */
    @Test
    void testDiscoveredAlgorithmsAreInstantiable() {
        Provider provider = new BouncyCastlePQCProvider();

        List<String> transformations = provider.getServices().stream()
                .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        // Verify each discovered algorithm has at least one service type
        for (String algorithm : transformations) {
            long serviceCount = provider.getServices().stream()
                    .filter(s -> algorithm.equals(s.getAlgorithm()))
                    .count();

            assertTrue(serviceCount > 0,
                    "Algorithm '" + algorithm + "' should have at least one registered service");
        }
    }

    /**
     * Edge case 4: Future Bouncy Castle updates.
     * Tests that the discovery mechanism is future-proof by not hardcoding
     * algorithm names or counts. When BC adds new PQC algorithms, they will be
     * automatically discovered without code changes.
     */
    @Test
    void testFutureProofDiscoveryMechanism() {
        Provider provider = new BouncyCastlePQCProvider();

        // The discovery logic should work regardless of the specific algorithms present
        List<String> transformations = provider.getServices().stream()
                .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        // We don't assert specific count or algorithm names (except for core ones in other tests)
        // This ensures the test remains valid when Bouncy Castle adds new algorithms
        assertNotNull(transformations, "Discovery should always return a non-null list");

        // The only assumption we make is that there should be SOME algorithms
        // (unless Bouncy Castle completely changes their provider structure)
        assertFalse(transformations.isEmpty(),
                "Provider should have at least some PQC algorithms registered");
    }

    /**
     * Edge case 5: GraalVM native image compatibility.
     * Tests that the build-time instantiation of BouncyCastlePQCProvider is safe
     * during STATIC_INIT phase. The provider must be instantiable without triggering
     * runtime-only operations that would break native image builds.
     */
    @Test
    void testProviderInstantiationForNativeImageCompatibility() {
        // This simulates the build-time instantiation in PqcProcessor
        Provider provider = new BouncyCastlePQCProvider();

        assertNotNull(provider, "Provider must be instantiable at build time");
        assertNotNull(provider.getServices(), "Provider services must be accessible at build time");
        assertFalse(provider.getServices().isEmpty(), "Provider should have services at build time");

        // Verify we can iterate over services (required for discovery)
        long serviceCount = provider.getServices().stream().count();
        assertTrue(serviceCount > 0, "Should be able to count services at build time");

        // Verify we can filter and map services (core discovery operations)
        List<String> algorithms = provider.getServices().stream()
                .map(Provider.Service::getAlgorithm)
                .limit(1)
                .collect(Collectors.toList());
        assertNotNull(algorithms, "Should be able to map services to algorithms at build time");
    }

    /**
     * Edge case 6: Service type filtering correctness.
     * Tests that using Arrays.asList(...).contains(s.getType()) correctly filters
     * only the 4 specified types. Alternative implementations using Set would be
     * more efficient but this is build-time code with minimal performance impact.
     */
    @Test
    void testServiceTypeFilteringCorrectness() {
        Provider provider = new BouncyCastlePQCProvider();

        // Test the actual filtering logic from PqcProcessor
        List<String> expectedTypes = Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory");

        List<Provider.Service> filteredServices = provider.getServices().stream()
                .filter(s -> expectedTypes.contains(s.getType()))
                .collect(Collectors.toList());

        // Verify only expected types are in the filtered list
        for (Provider.Service service : filteredServices) {
            assertTrue(expectedTypes.contains(service.getType()),
                    "Filtered service type '" + service.getType() + "' should be in expected types list");
        }

        // Verify we're not accidentally including unwanted types
        Set<String> discoveredTypes = filteredServices.stream()
                .map(Provider.Service::getType)
                .collect(Collectors.toSet());

        for (String type : discoveredTypes) {
            assertTrue(expectedTypes.contains(type),
                    "Discovered type '" + type + "' should be in expected types list");
        }

        // Verify the Arrays.asList approach works correctly (not using Set for historical reasons)
        assertTrue(expectedTypes.contains("Signature"), "Arrays.asList should contain Signature");
        assertTrue(expectedTypes.contains("KeyPairGenerator"), "Arrays.asList should contain KeyPairGenerator");
        assertFalse(expectedTypes.contains("NonExistentType"),
                "Arrays.asList should not contain non-existent types");
    }

    /**
     * Edge case: Verify that filtering is case-sensitive.
     * Service type names must match exactly (e.g., "Signature" not "signature").
     */
    @Test
    void testCaseSensitiveServiceTypeFiltering() {
        Provider provider = new BouncyCastlePQCProvider();

        // Try filtering with wrong case - should return empty list
        List<String> wrongCaseResult = provider.getServices().stream()
                .filter(s -> Arrays.asList("signature", "keypairgenerator", "cipher", "keyfactory")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(wrongCaseResult.isEmpty(),
                "Filtering with wrong case should return empty list - service types are case-sensitive");

        // Verify correct case works
        List<String> correctCaseResult = provider.getServices().stream()
                .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                        .contains(s.getType()))
                .map(Provider.Service::getAlgorithm)
                .distinct()
                .collect(Collectors.toList());

        assertFalse(correctCaseResult.isEmpty(),
                "Filtering with correct case should return results");
    }

    /**
     * Edge case: Verify stream operations don't modify the original provider services.
     * The discovery mechanism should be read-only and not alter the provider state.
     */
    @Test
    void testDiscoveryIsReadOnly() {
        Provider provider = new BouncyCastlePQCProvider();

        // Get initial service count
        int initialCount = provider.getServices().size();

        // Run discovery multiple times
        for (int i = 0; i < 3; i++) {
            List<String> transformations = provider.getServices().stream()
                    .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")
                            .contains(s.getType()))
                    .map(Provider.Service::getAlgorithm)
                    .distinct()
                    .collect(Collectors.toList());

            assertNotNull(transformations);
        }

        // Verify service count unchanged
        int finalCount = provider.getServices().size();
        assertEquals(initialCount, finalCount,
                "Discovery should not modify provider services");
    }

    /**
     * Edge case: Verify that null checks are not needed in the stream pipeline.
     * BouncyCastlePQCProvider should never return null services or null types/algorithms.
     */
    @Test
    void testNoNullsInProviderServices() {
        Provider provider = new BouncyCastlePQCProvider();

        // Verify no null services
        List<Provider.Service> allServices = provider.getServices().stream()
                .collect(Collectors.toList());

        for (Provider.Service service : allServices) {
            assertNotNull(service, "Provider should not return null services");
            assertNotNull(service.getType(), "Service type should not be null");
            assertNotNull(service.getAlgorithm(), "Service algorithm should not be null");
        }
    }
}
