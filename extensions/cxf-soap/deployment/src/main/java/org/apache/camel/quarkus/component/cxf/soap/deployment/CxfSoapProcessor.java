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
package org.apache.camel.quarkus.component.cxf.soap.deployment;

import java.util.Collections;
import java.util.stream.Stream;

import io.quarkiverse.cxf.deployment.CxfRouteRegistrationRequestorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class CxfSoapProcessor {

    private static final String FEATURE = "camel-cxf-soap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem ehcacheAgentSizeOfBypass() {
        return new SystemPropertyBuildItem("org.ehcache.sizeof.AgentSizeOf.bypass", "true");
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        IndexView index = combinedIndex.getIndex();

        Stream.of(
                "org.apache.wss4j.dom.handler.WSHandler") // can we remove this?
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).methods(false).fields(false).build())
                .forEach(reflectiveClass::produce);

        reflectiveMethods.produce(new ReflectiveMethodBuildItem("org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory",
                "getServiceFactory", new String[0]));
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("org.opensaml.core.xml.config.XMLObjectProviderRegistry").build());

    }

    @BuildStep
    CxfRouteRegistrationRequestorBuildItem requestCxfRouteRegistration() {
        return new CxfRouteRegistrationRequestorBuildItem(FEATURE);
    }

    @BuildStep
    RemovedResourceBuildItem removeEhcacheTokenStore() {
        return new RemovedResourceBuildItem(ArtifactKey.ga("org.ehcache", "ehcache"),
                Collections.singleton("org.ehcache.core.osgi.OsgiServiceLoader"));
    }

    @BuildStep
    RemovedResourceBuildItem removewssec() {
        return new RemovedResourceBuildItem(ArtifactKey.ga("org.apache.wss4j", "wss4j-ws-security-stax"),
                Collections.singleton("org.apache.wss4j.stax.setup.WSSec"));
    }

    @BuildStep
    void removeEhcacheTokenStore(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        //        runtimeInitialized
        //                .produce(new RuntimeInitializedClassBuildItem("org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder"));
        //        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(
        //                "org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder "));
        //            runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(
        //                    "org.apache.cxf.ws.security.policy.interceptors.SpnegoContextTokenInInterceptor"));
    }
}
