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
package org.apache.camel.quarkus.test.support.pqc.deployment;

import java.security.KeyPair;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPair;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPairRecorder;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPairs;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * Quarkus build step processor for auto-registering PQC keypairs as named CDI beans.
 * Scans for {@link PQCKeyPairs} annotations and creates synthetic beans for each
 * declared keypair, making them injectable via {@code @Inject @Named}.
 */
public class PQCKeyPairBuildStep {
    private static final Logger LOGGER = Logger.getLogger(PQCKeyPairBuildStep.class);

    private static final DotName PQC_KEY_PAIRS = DotName.createSimple(PQCKeyPairs.class.getName());

    /**
     * Scans for @PQCKeyPairs annotations and registers synthetic beans for each declared keypair.
     * Keypairs are generated at STATIC_INIT time via the recorder.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerPQCKeyPairBeans(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            PQCKeyPairRecorder recorder) {

        IndexView index = combinedIndex.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(PQC_KEY_PAIRS);

        if (annotations.isEmpty()) {
            LOGGER.debug("No @PQCKeyPairs annotations found - skipping PQC keypair bean registration");
            return;
        }

        // Use map to deduplicate keypairs with the same name across multiple test classes
        Map<String, KeyPairConfig> uniqueKeyPairs = new HashMap<>();

        for (AnnotationInstance annotationInstance : annotations) {
            AnnotationValue keyPairsValue = annotationInstance.value("keyPairs");
            if (keyPairsValue == null) {
                continue;
            }

            AnnotationInstance[] keyPairAnnotations = keyPairsValue.asNestedArray();

            for (AnnotationInstance keyPairAnnotation : keyPairAnnotations) {
                String name = keyPairAnnotation.value("name").asString();
                String algorithm = keyPairAnnotation.value("algorithm").asEnum();

                // Convert enum name to algorithm name (e.g., DILITHIUM2 -> Dilithium2)
                String algorithmName = PQCAlgorithm.valueOf(algorithm).getAlgorithmName();

                // Check for duplicates
                KeyPairConfig existing = uniqueKeyPairs.get(name);
                if (existing != null) {
                    if (!existing.algorithmName.equals(algorithmName)) {
                        LOGGER.warnf("Duplicate keypair name '%s' with different algorithms: %s vs %s. Using first declaration.",
                                name, existing.algorithmName, algorithmName);
                    }
                    continue; // Skip duplicate
                }

                uniqueKeyPairs.put(name, new KeyPairConfig(name, algorithmName));
            }
        }

        // Create synthetic beans for each unique keypair
        for (KeyPairConfig config : uniqueKeyPairs.values()) {
            LOGGER.infof("Registering PQC keypair bean: name='%s', algorithm='%s'",
                    config.name, config.algorithmName);

            syntheticBeans.produce(
                    SyntheticBeanBuildItem.configure(KeyPair.class)
                            .scope(Singleton.class)
                            .named(config.name)
                            .runtimeValue(recorder.generateKeyPair(config.algorithmName))
                            .unremovable()
                            .done());
        }

        LOGGER.infof("Registered %d PQC keypair bean(s)", uniqueKeyPairs.size());
    }

    /**
     * Internal helper class to track keypair configuration
     */
    private static class KeyPairConfig {
        final String name;
        final String algorithmName;

        KeyPairConfig(String name, String algorithmName) {
            this.name = name;
            this.algorithmName = algorithmName;
        }
    }
}
