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
package org.apache.camel.quarkus.component.djl.deployment;

import java.io.IOException;

import ai.djl.repository.zoo.LocalZooProvider;
import ai.djl.repository.zoo.ZooProvider;
import com.google.gson.JsonSerializer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class DjlProcessor {

    private static final String FEATURE = "camel-djl";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(false, false,
                JsonSerializer.class.getCanonicalName());
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> services) throws IOException {
        //        String service = "META-INF/services/" + ZooProvider.class.getName();
        //
        //        // find out all the implementation classes listed in the service files
        //        Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
        //                service);
        //
        //        System.out.println("--------------------------------");
        //        System.out.println("registering for: " + ZooProvider.class.getName());
        //        System.out.println(implementations);
        //        System.out.println("--------------------------------");
        // register every listed implementation class so they can be instantiated
        // in native-image at run-time
        //        services.produce(
        //                new ServiceProviderBuildItem(ZooProvider.class.getName(),
        //                        implementations.toArray(new String[0])));

        System.out.println("--------------------------------");
        System.out.println("registering for: " + ZooProvider.class.getName());
        System.out.println(LocalZooProvider.class.getName());
        System.out.println("--------------------------------");
        services.produce(
                new ServiceProviderBuildItem(ZooProvider.class.getName(), LocalZooProvider.class.getName()));

    }
}
