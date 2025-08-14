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
package org.apache.camel.quarkus.support.mysql.connector.deployment;

import java.util.HashMap;
import java.util.Map;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SupportMysqlConnectorProcessor {

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem(AbandonedConnectionCleanupThread.class.getCanonicalName());
    }

    @BuildStep
    void generateIfMissing(CombinedIndexBuildItem indexBuildItem, NativeConfig nativeConfig,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        //in native mode and if `com.oracle.bmc.ConfigFileReader` is not present,
        //generate those empty classes to avoid a noTypeDefError thrown from `AuthenticationOciClient`
        IndexView index = indexBuildItem.getIndex();
        if (nativeConfig.enabled() && index.getClassByName(
                DotName.createSimple("com.mysql.cj.protocol.a.authentication.AuthenticationOciClient")) == null) {

            generatedClasses.produce(generateClass("com.oracle.bmc.ConfigFileReader"));
            generatedClasses.produce(generateClass("com.oracle.bmc.ConfigFileReader$ConfigFile"));
        }
    }

    private GeneratedClassBuildItem generateClass(String className) {

        Map<String, byte[]> generated = new HashMap<>();

        ClassOutput output = (name, data) -> generated.put(name, data);

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(output)
                .className(className)
                .build()) {
            //empty classes
        }

        byte[] clazz = generated.get(className.replace('.', '/'));

        return new GeneratedClassBuildItem(true, className, clazz);
    }
}
