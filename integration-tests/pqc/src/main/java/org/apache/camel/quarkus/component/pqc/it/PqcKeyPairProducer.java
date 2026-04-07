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

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * CDI producer for PQC keypairs used in integration tests.
 * Generates keypairs for various Post-Quantum Cryptography algorithms using BouncyCastle PQC provider.
 */
@ApplicationScoped
public class PqcKeyPairProducer {

    private static final String BCPQC_PROVIDER = "BCPQC";

    @Produces
    @Named("dilithiumKeyPair")
    @Singleton
    public KeyPair produceDilithiumKeyPair() {
        return generateKeyPair("Dilithium2");
    }

    @Produces
    @Named("falconKeyPair")
    @Singleton
    public KeyPair produceFalconKeyPair() {
        return generateKeyPair("Falcon-512");
    }

    @Produces
    @Named("sphincsKeyPair")
    @Singleton
    public KeyPair produceSPHINCSKeyPair() {
        return generateKeyPair("SPHINCSPlus");
    }

    @Produces
    @Named("xmssKeyPair")
    @Singleton
    public KeyPair produceXMSSKeyPair() {
        return generateKeyPair("XMSS");
    }

    @Produces
    @Named("lmsKeyPair")
    @Singleton
    public KeyPair produceLMSKeyPair() {
        return generateKeyPair("LMS");
    }

    @Produces
    @Named("kyberKeyPair")
    @Singleton
    public KeyPair produceKyberKeyPair() {
        return generateKeyPair("Kyber512");
    }

    private KeyPair generateKeyPair(String algorithmName) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithmName, BCPQC_PROVIDER);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PQC keypair: " + algorithmName, e);
        }
    }
}
