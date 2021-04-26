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
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.api.client.util.GenericData;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageRequest;
import com.google.cloud.storage.Bucket;
import io.quarkiverse.googlecloudservices.storage.runtime.StorageProducer;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
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
    void registerForReflection(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        IndexView index = combinedIndex.getIndex();

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, true, Bucket.class.getName(),
                Storage.Objects.Insert.class.getName(),
                StorageRequest.class.getName(), GenericJson.class.getName(), GenericData.class.getName(),
                GoogleJsonError.class.getName(), HttpHeaders.class.getName(), JsonWebToken.Payload.class.getName(),
                JsonWebSignature.Header.class.getName(), GoogleJsonError.ErrorInfo.class.getName(), byte[].class.getName()));

        String[] dtos = index.getAllKnownSubclasses(DotName.createSimple(StorageRequest.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .toArray(String[]::new);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, true, dtos));

        dtos = index.getAllKnownSubclasses(DotName.createSimple(GenericJson.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .toArray(String[]::new);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, true, dtos));
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.google.apis", "google-api-services-storage");
    }

    UnremovableBeanBuildItem create() {
        return UnremovableBeanBuildItem.beanTypes(StorageProducer.class);
    }
}
