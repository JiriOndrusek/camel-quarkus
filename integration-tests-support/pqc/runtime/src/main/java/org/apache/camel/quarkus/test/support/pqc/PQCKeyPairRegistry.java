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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for storing PQC keypairs generated at build time.
 * This avoids embedding large keypair bytes directly in bytecode.
 */
public class PQCKeyPairRegistry {
    private static final Map<String, KeyPair> KEY_PAIRS = new ConcurrentHashMap<>();

    private PQCKeyPairRegistry() {
    }

    /**
     * Registers a keypair with the given name.
     *
     * @param name    unique name for the keypair
     * @param keyPair the keypair to register
     */
    public static void register(String name, KeyPair keyPair) {
        KEY_PAIRS.put(name, keyPair);
    }

    /**
     * Retrieves a keypair by name.
     *
     * @param name the keypair name
     * @return the keypair, or null if not found
     */
    public static KeyPair get(String name) {
        return KEY_PAIRS.get(name);
    }

    /**
     * Clears all registered keypairs (mainly for testing).
     */
    public static void clear() {
        KEY_PAIRS.clear();
    }
}
