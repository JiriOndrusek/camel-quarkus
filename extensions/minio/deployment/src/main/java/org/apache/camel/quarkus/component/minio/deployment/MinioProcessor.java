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
package org.apache.camel.quarkus.component.minio.deployment;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import io.minio.BaseArgs;
import io.minio.messages.Item;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Converter;

class MinioProcessor {

    private static final String FEATURE = "camel-minio";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflectionWithFields(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        LinkedList<String> dtos = new LinkedList();

        dtos.add(Item.class.getName());
        dtos.addAll(index.getAnnotations(DotName.createSimple(Root.class.getName())).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(a -> a.target().asClass().name().toString())
                .collect(Collectors.toList()));

        return new ReflectiveClassBuildItem(false, true, dtos.toArray(new String[dtos.size()]));
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Stream<ClassInfo> converters = index.getAllKnownImplementors(DotName.createSimple(Converter.class.getName())).stream();
        Stream<ClassInfo> baseArgs = index.getAllKnownSubclasses(DotName.createSimple(BaseArgs.class.getName())).stream();
        Stream<ClassInfo> labels = index.getAllKnownImplementors(DotName.createSimple("org.simpleframework.xml.core.Label"))
                .stream();

        String[] dtos = Streams.concat(labels, Stream.concat(converters, baseArgs))
                .map(ci -> ci.name().toString())
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, false, dtos);
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.minio", "minio"));
        indexDependency.produce(new IndexDependencyBuildItem("com.carrotsearch.thirdparty", "simple-xml-safe"));
    }

}
