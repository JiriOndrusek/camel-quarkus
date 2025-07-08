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
package org.apache.camel.quarkus.component.mail.microsoft.oauth.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class MailMicrosoftOauthProcessor {

    private static final Logger LOG = Logger.getLogger(MailMicrosoftOauthProcessor.class);
    private static final String FEATURE = "camel-mail-microsoft-oauth";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSSLNativeSupport() {
        // Required by TimeZoneUpdater$UrlBuilder.toUrl
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    //        //Cannot construct instance of `com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse`
    //        @BuildStep
    //        ReflectiveClassBuildItem registerForReflection() {
    //            return ReflectiveClassBuildItem.builder("com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse")
    //                    .build();
    //        }

    //    //hunch com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse
    //    @BuildStep
    //    ReflectiveClassBuildItem registerAadInstanceDiscoveryResponseForReflection() {
    //        return ReflectiveClassBuildItem.builder("com.microsoft.aad.msal4j.OidcDiscoveryResponse").fields().methods()
    //                .build();
    //    }

    //[Correlation ID: 94babc5f-8706-4058-9045-fcf56162f9fd] Execution of class com.microsoft.aad.msal4j.AcquireTokenByClientCredentialSupplier failed: null

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        //todo fuse only required ones
        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("com.microsoft.aad.msal4j"))
                .sorted()
                .peek(System.out::println)
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(dtos).methods().fields().build();
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.microsoft.azure", "msal4j");
    }

}
