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
package org.apache.camel.quarkus.component.avro.rpc.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.avro.ipc.HandshakeMatch;
import org.apache.avro.ipc.HandshakeRequest;
import org.apache.avro.ipc.HandshakeResponse;
import org.jboss.jandex.IndexView;

class AvroRpcProcessor {

    private static final String FEATURE = "camel-avro-rpc";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        //
        //        Collection<AnnotationInstance> annotations = indexBuildItem.getIndex()
        //                .getAnnotations(DotName.createSimple(AvroGenerated.class.getName()));
        //        for (AnnotationInstance annotation : annotations) {
        //            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
        //                String className = annotation.target().asClass().name().toString();
        //                builder.addRuntimeInitializedClass(className);
        //                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, className));
        //            }
        //        }
        //        String[] dtos = index.getAnnotations(DotName.createSimple(AvroGenerated.class.getName())).stream()
        //                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
        //                .map(a -> a.target().asClass().name().toString())
        //                //                .map(ci -> ci.name().toString())
        //                .sorted() //todo remove
        //                .peek(System.out::println) //todo remove
        //                .toArray(String[]::new);

        //        return new ReflectiveClassBuildItem(true, true, dtos);

        return new ReflectiveClassBuildItem(true, true, HandshakeMatch.class, HandshakeRequest.class, HandshakeResponse.class);

    }

    @BuildStep
    void registerDependencyForIndex(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.avro", "avro-ipc-netty"));
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.avro", "avro-ipc-jetty"));
    }
}
