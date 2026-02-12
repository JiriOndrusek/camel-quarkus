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
package org.apache.camel.quarkus.component.langchain4j.agent.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.langchain4j.guardrail.Guardrail;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.agent.QuarkusLangchain4jRecorder;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class Langchain4jAgentProcessor {
    private static final String FEATURE = "camel-langchain4j-agent";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        Set<String> mcpProtocolClasses = combinedIndex.getIndex()
                .getClassesInPackage("dev.langchain4j.mcp.protocol")
                .stream()
                .map(ClassInfo::asClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        return ReflectiveClassBuildItem
                .builder(mcpProtocolClasses.toArray(new String[0]))
                .fields(true)
                .methods(true)
                .build();
    }

    @BuildStep(onlyIf = QuarkusLangchain4jPresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    void specifyHttpClient(QuarkusLangchain4jRecorder recorder) {
        recorder.enforceJaxRsHttpClient();
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerLangChain4jAiServiceTypesForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            QuarkusLangchain4jRecorder recorder) {
        IndexView index = combinedIndex.getIndex();
        // Guardrails are instantiated dynamically
        Set<DotName> guardrailTypes = index.getAllKnownImplementations(InputGuardrail.class)
                .stream()
                .map(ClassInfo::name)
                .collect(Collectors.toSet());

        index.getAllKnownImplementations(OutputGuardrail.class)
                .stream()
                .map(ClassInfo::name)
                .forEach(guardrailTypes::add);

        guardrailTypes.stream()
                .filter(s -> !s.toString().equals("dev.langchain4j.guardrail.JsonExtractorOutputGuardrail"))
                .forEach(s -> {
                    try {
                        Class<Guardrail<?, ?>> guardrailClass;
                        guardrailClass = (Class<Guardrail<?, ?>>) Thread.currentThread()
                                .getContextClassLoader()
                                .loadClass(s.toString());
                        syntheticBeans
                                .produce(SyntheticBeanBuildItem.configure(s)
                                        .scope(Singleton.class)
                                        .named("GuardrailSynthetic" + s.local())
                                        .runtimeValue(recorder.instantiateGuardrails(guardrailClass))
                                        .done());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                });

    }

}
