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
package org.apache.camel.quarkus.test.support.pqc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

/**
 * Quarkus recorder for generating PQC keypairs at static init time.
 * This class is invoked during the Quarkus build process to create
 * runtime values that will be used to instantiate CDI beans.
 */
@Recorder
public class PQCKeyPairRecorder {
    private static final String BCPQC_PROVIDER = "BCPQC";

    /**
     * Generates a PQC keypair at STATIC_INIT time and wraps it in a RuntimeValue.
     *
     * @param  algorithmName PQC algorithm name (e.g., "Dilithium2")
     * @return               RuntimeValue containing the generated KeyPair
     */
    public RuntimeValue<KeyPair> generateKeyPair(String algorithmName) {
        try {
            // Ensure BCPQC provider is registered
            if (Security.getProvider(BCPQC_PROVIDER) == null) {
                Security.addProvider(new BouncyCastlePQCProvider());
            }

            //Generate keypair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithmName, BCPQC_PROVIDER);
            KeyPair keyPair = kpg.generateKeyPair();

            return new RuntimeValue<>(keyPair);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to generate PQC keypair with algorithm '%s'", algorithmName),
                    e);
        }
    }
}
