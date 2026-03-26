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
package org.apache.camel.quarkus.component.pqc;

import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.List;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.jboss.logging.Logger;

@Recorder
public class PqcRecorder {

    private static final Logger LOG = Logger.getLogger(PqcRecorder.class);
    private static final String PROVIDER_NAME = "BCPQC";

    public void registerBouncyCastlePQCProvider(List<String> cipherTransformations, ShutdownContext shutdownContext) {
        Provider provider = Security.getProvider(PROVIDER_NAME);
        if (provider == null) {
            provider = new BouncyCastlePQCProvider();
            try {
                Security.addProvider(provider);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }
            LOG.debugf("Registered BouncyCastlePQCProvider");
        }

        // Make it explicit to the static analysis that PQC algorithms are reachable at runtime
        for (String transformation : cipherTransformations) {
            try {

            } catch (Exception e) {
                // The algorithm is not present, a runtime error will be reported as usual
                LOG.errorf("Could not instantiate %s: %s", transformation, e.getMessage());
            }
        }

        shutdownContext.addShutdownTask(() -> {
            Security.removeProvider(PROVIDER_NAME);
            LOG.debug("Removed BouncyCastlePQC security provider");
        });
    }
}
