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

import java.util.Arrays;
import java.util.stream.Stream;

import io.minio.BaseArgs;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.simpleframework.xml.Root;

class MinioProcessor {

    private static final String FEATURE = "camel-minio";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos2 = index.getAllKnownSubclasses(DotName.createSimple(BaseArgs.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .sorted()
                .peek(o -> System.out.println("2- " + o))
                .toArray(String[]::new);

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("io.minio.messages"))
                .sorted()
                .peek(o -> System.out.println("1- " + o))
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(true, true, Stream.concat(Arrays.stream(dtos), Arrays.stream(dtos2))
                .toArray(String[]::new));
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection1(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        //        String[] dtos2 = index.getKnownClasses().stream()
        //                .map(ci -> ci.name().toString())
        //                .filter(n -> n.startsWith("org.simpleframework.xml"))
        //                .sorted()
        //                .peek(System.out::println)
        //                .toArray(String[]::new);

        String[] dtos2 = index.getAllKnownImplementors(DotName.createSimple("org.simpleframework.xml.core.Label")).stream()
                .map(ci -> ci.name().toString())
                .sorted()
                .peek(System.out::println)
                .toArray(String[]::new);

        //
        //        String[] dtos2 = index.getAllKnownImplementors(DotName.createSimple(Converter.class.getName())).stream()
        //                .map(ci -> ci.name().toString())
        //                .sorted()
        //                .peek(System.out::println)
        //                .toArray(String[]::new);

        //        String[] dtos = Stream
        //                .concat(index.getAllKnownSubclasses(DotName.createSimple(BaseArgs.class.getName())).stream(),
        //                        index.getAllKnownImplementors(DotName.createSimple(Converter.class.getName())).stream())
        //                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(true, true, dtos2);
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("io.minio", "minio");
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex2() {
        return new IndexDependencyBuildItem("com.carrotsearch.thirdparty", "simple-xml-safe");
    }
    //
    //    @BuildStep
    //    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
    //        indexDependency.produce(new IndexDependencyBuildItem("org.jboss.logging", "commons-logging-jboss-logging"));
    //        indexDependency.produce(new IndexDependencyBuildItem("org.apache.xmlgraphics", "fop"));

    @BuildStep
    ReflectiveClassBuildItem registerForReflection2(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageSystemPropertyBuildItem> sys,
            BuildProducer<NativeImageConfigBuildItem> conf) {
        IndexView index = combinedIndex.getIndex();

        //        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();
        //        Collection<AnnotationInstance> annotations = index
        //                .getAnnotations(DotName.createSimple(Root.class.getName()));
        //                for (AnnotationInstance annotation : annotations) {
        //                    if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
        //                        String className = annotation.target().asClass().name().toString();
        //                        builder.addRuntimeInitializedClass(className);
        //                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, className));
        //                    }
        //                }
        String[] dtos = index.getAnnotations(DotName.createSimple(Root.class.getName())).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(a -> a.target().asClass().name().toString())
                //                .map(ci -> ci.name().toString())
                .sorted() //todo remove
                .peek(o -> System.out.println("-- " + o)) //todo remove
                .toArray(String[]::new);

        //                return new ReflectiveClassBuildItem(true, true, dtos);

        return new ReflectiveClassBuildItem(true, true, dtos);

    }

}
