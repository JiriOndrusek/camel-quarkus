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
package org.apache.camel.quarkus.component.google.storage.deployment;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.GenericData;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageRequest;
import com.google.cloud.storage.Bucket;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.commons.lang3.ArrayUtils;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class GoogleStorageProcessor {

    private static final String FEATURE = "camel-google-storage";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem enableAllSecurity() {
        return new EnableAllSecurityServicesBuildItem();
    }

    //todo refactor
    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {

        return new ReflectiveClassBuildItem(true, true, Bucket.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection2() {
        return new ReflectiveClassBuildItem(true, true, Storage.Objects.Insert.class.getName(),
                StorageRequest.class.getName(), GenericJson.class.getName(), GenericData.class.getName(),
                GoogleJsonError.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection3(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getAllKnownSubclasses(DotName.createSimple(StorageRequest.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .sorted() //todo remove
                .peek(System.out::println) //todo remove
                .toArray(String[]::new);

        String[] dtos2 = index.getAllKnownSubclasses(DotName.createSimple(GenericJson.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .sorted() //todo remove
                .peek(System.out::println) //todo remove
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(true, true, ArrayUtils.addAll(dtos, dtos2));
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.google.apis", "google-api-services-storage");
    }

}
