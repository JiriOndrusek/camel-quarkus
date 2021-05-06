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
package org.apache.camel.quarkus.it.support.typeconverter.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterLoaderBuildItem;
import org.apache.camel.quarkus.it.support.typeconverter.CustomTypeConverterRecorder;

public class CustomTypeConverterBuildStep {
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelTypeConverterLoaderBuildItem typeConverterLoader(CustomTypeConverterRecorder recorder) {
        return new CamelTypeConverterLoaderBuildItem(recorder.createTypeConverterLoader());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void bindMyStringConverter(CustomTypeConverterRecorder recorder,
            CamelRegistryBuildItem registryBuildItem) {
        recorder.bindMyStringConverter(registryBuildItem.getRegistry());
    }

    //    //    @BuildStep //todo try to use CamelTypeConverterLoaderBuildItem
    //    //    ReflectiveClassBuildItem registerForReflection() {
    //    //        return new ReflectiveClassBuildItem(false, false, TypeConverter.class.getName());
    //    //    }
    //
    //    @Record(ExecutionTime.STATIC_INIT)
    //    @BuildStep
    //    void registerForReflection(CustomTypeConverterRecorder recorder,
    //            CombinedIndexBuildItem combinedIndex,
    //            CamelTypeConverterRegistryBuildItem camelTypeConverterRegistryBuildItem) {
    //        IndexView index = combinedIndex.getIndex();
    //
    //        Set<ClassInfo> converterClasses = new HashSet<>();
    //        Collection<AnnotationInstance> annotations = index
    //                .getAnnotations(DotName.createSimple(Converter.class.getName()));
    //        for (AnnotationInstance annotation : annotations) {
    //            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
    //                recorder.bindCustomConverter(camelTypeConverterRegistryBuildItem.getRegistry(),
    //                        annotation.target().asClass().name().toString());
    //            }
    //            //            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
    //            //                System.out.println(
    //            //                        ">>>>> class: " + annotation.target().asClass().name().toString());
    //            //                converterClasses.add(annotation.target().asClass());
    //            //                continue;
    //            //            }
    //            //
    //            //            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD && converterClasses.contains( annotation.target().asMethod().declaringClass())) {
    //            //                System.out.println(
    //            //                        ">>>>> method: " + annotation.target().asMethod().name());
    //            //                Class clazz = Class.forName(annotation.target().asMethod().declaringClass().name().toString());
    //            //                annotation.target().asMethod().mename(), annotation.target().asMethod().p)
    //            //
    //            //                return new CamelTypeConverterLoaderBuildItem(
    //            //                        annotation.target().asMethod().
    //            //                     recorder.bindCustomConverter()
    //            //                );
    //            //            }
    //            //            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
    //            //                String className = annotation.target().asClass().name().toString();
    //            //
    //            //                reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, className));
    //            //                //                typeConverterLoaders
    //            //                //                        .produce(new CamelTypeConverterLoaderBuildItem(new RuntimeValue<TypeConverterLoader>(registry -> {
    //            //                //                            try {
    //            //                //                                registry.addTypeConverter(MyString.class, String.class,
    //            //                //                                        (TypeConverter) Class.forName(className).getDeclaredConstructor().newInstance());
    //            //                //                            } catch (Exception e) {
    //            //                //                                e.printStackTrace();
    //            //                //                            }
    //            //                //                        })));
    //            //
    //            //                //                typeConverterLoaders
    //            //                //                        .produce(new CamelTypeConverterLoaderBuildItem(new RuntimeValue<>(new MyStringConverterLoader())));
    //            //
    //            //                recorder.bindCustomConverter(className, registryBuildItem.getRegistry());
    //            //            }
    //        }
    //    }
}
