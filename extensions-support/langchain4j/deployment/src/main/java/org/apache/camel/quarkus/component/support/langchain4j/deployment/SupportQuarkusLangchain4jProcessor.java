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
package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.langchain4j.guardrail.Guardrail;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.support.langchain4j.CamelToolProvider;
import org.apache.camel.quarkus.component.support.langchain4j.QuarkusLangchain4jRecorder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static io.quarkus.arc.deployment.UnremovableBeanBuildItem.beanClassNames;

/**
 * Build steps required only when Quarkus LangChain4j is detected.
 */
@BuildSteps(onlyIf = QuarkusLangchain4jPresent.class)
class SupportQuarkusLangchain4jProcessor {

    public static final DotName REGISTER_AI_SERVICES_DOTNAME = DotName
            .createSimple("io.quarkiverse.langchain4j.RegisterAiService");
    private static final DotName CAMEL_TOOLS_DOTNAME = DotName
            .createSimple("org.apache.camel.quarkus.component.support.langchain4j.CamelTools");

    private static final Logger LOG = Logger.getLogger(SupportQuarkusLangchain4jProcessor.class);

    @BuildStep
    SystemPropertyBuildItem enforceJaxRsHttpClient() {
        LOG.infof("Quarkus LangChain4j detected - enforcing JAX-RS HTTP client factory");
        return new SystemPropertyBuildItem("langchain4j.http.clientBuilderFactory",
                "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory");
    }

    @BuildStep
    @SuppressWarnings("unchecked")
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

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerQuarkusLangchain4jNativeSupport(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView index = combinedIndex.getIndex();

        // Discover all inner classes of QuarkusJsonCodecFactory
        List<String> codecFactoryClasses = index.getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(n -> n.startsWith("io.quarkiverse.langchain4j.QuarkusJsonCodecFactory"))
                .toList();

        LOG.infof("Registered %d QuarkusJsonCodecFactory-related classes for native reflection",
                codecFactoryClasses.size());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                codecFactoryClasses.toArray(new String[0]))
                .methods()
                .fields()
                .constructors()
                .build());
    }

    @BuildStep(onlyIf = AiToolPresent.class)
    AdditionalBeanBuildItem registerCamelToolProvider() {
        LOG.info("Camel AI Tool detected - registering CamelToolProvider as CDI bean for ToolProvider auto-discovery");
        return AdditionalBeanBuildItem.unremovableOf(CamelToolProvider.class);
    }

    @BuildStep(onlyIf = AiToolPresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    void configureCamelToolTag(
            CombinedIndexBuildItem combinedIndex,
            QuarkusLangchain4jRecorder recorder) {

        IndexView index = combinedIndex.getIndex();
        Set<String> tags = index.getAnnotations(CAMEL_TOOLS_DOTNAME).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(a -> a.value().asString())
                .collect(Collectors.toSet());

        if (tags.size() > 1) {
            throw new RuntimeException(
                    "Multiple @CamelTools annotations with different tag values found: " + tags
                            + ". Only one tag per application is supported. "
                            + "For multi-tag support, use custom Supplier<ToolProvider> implementations.");
        }

        if (tags.size() == 1) {
            String tag = tags.iterator().next();
            LOG.infof("Configuring CamelToolProvider with tag filter: %s", tag);
            recorder.setCamelToolTag(tag);
        }
    }

    @BuildStep
    void markAiServicesAsUnremovable(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        LOG.debug("Discovering classes annotated with @RegisterAiService to mark implementation beans as unremovable");

        for (AnnotationInstance instance : indexBuildItem.getIndex().getAnnotations(REGISTER_AI_SERVICES_DOTNAME)) {
            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                String declarativeAiServiceClassName = instance.target().asClass().name().toString();
                LOG.debugf("Marking Quarkus Ai service implementation class for %s as unremovable",
                        declarativeAiServiceClassName);
                unremovableBeans.produce(beanClassNames(declarativeAiServiceClassName + "$$QuarkusImpl"));
            }
        }
    }
}
