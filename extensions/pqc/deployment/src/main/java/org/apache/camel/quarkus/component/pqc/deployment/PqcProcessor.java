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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

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
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.jboss.jandex.ClassInfo;
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
                .filter(
                        n -> (n.startsWith("org.bouncycastle.pqc.jcajce.provider") && n.endsWith("Spi") || n.contains("Spi$")))
                .toArray(String[]::new);

        LOG.infof("Discovered %d PQC classes", pqcClasses.length);

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
    void registerBouncyCastlePQCProvider(PqcRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        List<String> algorithms = index.getKnownClasses().stream()
                .filter(ci -> ci.name().toString().startsWith("org.bouncycastle.pqc.jcajce.provider.") &&
                        ci.name().toString().contains("$Mappings"))
                .map(ci -> ci.enclosingClass().toString()
                        .substring(ci.enclosingClass().toString().lastIndexOf('.') + 1))
                .toList();

        List<String> keyPairGenerators = index
                .getAllKnownImplementations(AsymmetricCipherKeyPairGenerator.class.getName()).stream()
                .filter(ci -> ci.name().toString().startsWith("org.bouncycastle.pqc.crypto."))
                .map(ClassInfo::simpleName)
                .toList();

        List<String> transformations = new LinkedList<>(Stream.concat(algorithms.stream(), keyPairGenerators.stream())
                .toList());
        transformations.add("org.bouncycastle.pqc.jcajce.provider.xmss.XMSSSignatureSpi$generic");

        LOG.infof("Discovered %d PQC algorithms from BouncyCastlePQCProvider", transformations.size());

        recorder.registerBouncyCastlePQCProvider(transformations, shutdownContextBuildItem);
    }

}
