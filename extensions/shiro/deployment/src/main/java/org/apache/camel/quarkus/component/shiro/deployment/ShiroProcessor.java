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
package org.apache.camel.quarkus.component.shiro.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.CamelAuthorizationException;
import org.jboss.jandex.IndexView;

class ShiroProcessor {

    private static final String FEATURE = "camel-shiro";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> (n.startsWith("org.apache.shiro.auth") && n.endsWith("Exception"))
                /*|| n.startsWith("org.apache.commons.configuration2")*/)
                .sorted()
                .peek(System.out::println)
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, false, dtos);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(false, false, CamelAuthorizationException.class.getName());
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.shiro", "shiro-core");
    }

    //    @BuildStep
    //    IndexDependencyBuildItem registerDependencyForIndex2() {
    //        return new IndexDependencyBuildItem("org.apache.commons", "commons-configuration2");
    //    }

    //
    //    @BuildStep
    //    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        resource.produce(new RuntimeInitializedClassBuildItem("java.lang.Float"));
    //    }

    @BuildStep
    public void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("java.lang.Float[]",
                        "java.util.Date[]")
                .build());
    }
}
