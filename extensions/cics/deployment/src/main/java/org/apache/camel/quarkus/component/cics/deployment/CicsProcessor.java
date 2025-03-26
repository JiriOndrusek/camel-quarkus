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
package org.apache.camel.quarkus.component.cics.deployment;

import java.util.ListResourceBundle;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class CicsProcessor {

    private static final Logger LOG = Logger.getLogger(CicsProcessor.class);
    private static final String FEATURE = "camel-cics";
    private static final DotName LIST_RESOURCE_BUNDLE_NAME = DotName.createSimple(ListResourceBundle.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem
                .builder(DefaultEvictionPolicy.class.getName(), "com.ibm.ctg.client.ClientResourceBundle")
                .build();
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.ibm", "ctgclient");
    }

    @BuildStep
    void resourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> imageResourceBundles,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getAllKnownSubclasses(LIST_RESOURCE_BUNDLE_NAME).stream()
                .filter(cl -> cl.name().toString().contains("CicsResourceBundle")
                        || cl.name().toString().contains("ClientResourceBundle"))
                .peek(System.out::println)
                .map(c -> new NativeImageResourceBundleBuildItem(c.name().toString()))
                .forEach(imageResourceBundles::produce);
    }
}
