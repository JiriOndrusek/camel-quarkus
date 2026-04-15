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

import java.util.Set;
import java.util.stream.Collectors;

import dev.langchain4j.guardrail.Guardrail;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import jakarta.inject.Singleton;
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

    private static final Logger LOG = Logger.getLogger(SupportQuarkusLangchain4jProcessor.class);

    @BuildStep
    SystemPropertyBuildItem enforceJaxRsHttpClient() {
        LOG.infof("Quarkus LangChain4j detected - enforcing JAX-RS HTTP client factory");
        return new SystemPropertyBuildItem("langchain4j.http.clientBuilderFactory",
                "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory");
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

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerLangchain4jRuntimeInitialization(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        // OutputGuardrailExecutor creates mutable collections at runtime
        // that need runtime initialization to avoid UnsupportedOperationException
        // when guardrails try to add elements to build-time initialized collections
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "dev.langchain4j.guardrail.OutputGuardrailExecutor"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerQuarkusLangchain4jNativeSupport(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            BuildProducer<NativeImageResourceBuildItem> nativeResources) {

        IndexView index = combinedIndex.getIndex();

        // Register QL4J's @RegisterAiService implementations for reflection
        for (AnnotationInstance instance : index.getAnnotations(REGISTER_AI_SERVICES_DOTNAME)) {
            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                String serviceName = instance.target().asClass().name().toString();
                LOG.debugf("Registering QL4J AI service %s for native reflection", serviceName);

                // QL4J generates implementation classes with $$QuarkusImpl suffix
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(serviceName + "$$QuarkusImpl")
                        .methods()
                        .fields()
                        .build());

                // Register the interface as proxy
                proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(serviceName));
            }
        }

        // Register JAX-RS HTTP client classes for reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory")
                .methods()
                .build());

        // Register ChatMessage polymorphic subtypes for QL4J JSON deserialization
        // Note: QL4J uses its own QuarkusChatMessageJsonCodecFactory which requires
        // full serialization support for polymorphic types
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "dev.langchain4j.data.message.AiMessage",
                "dev.langchain4j.data.message.SystemMessage",
                "dev.langchain4j.data.message.UserMessage",
                "dev.langchain4j.data.message.ToolExecutionResultMessage",
                "dev.langchain4j.data.message.ChatMessage",
                "dev.langchain4j.data.message.ChatMessage$Type",
                "dev.langchain4j.data.message.AiMessage$Builder",
                "dev.langchain4j.data.message.UserMessage$Builder",
                "dev.langchain4j.data.message.ToolExecutionResultMessage$Builder",
                "dev.langchain4j.agent.tool.ToolExecutionRequest$Builder")
                .methods()
                .fields()
                .serialization()
                .constructors()
                .build());

        // Register tool-related classes for native mode
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "dev.langchain4j.agent.tool.ToolExecutionRequest",
                "dev.langchain4j.agent.tool.ToolSpecification",
                "dev.langchain4j.agent.tool.ToolParameters")
                .methods()
                .fields()
                .serialization()
                .build());

        // Register QL4J JSON codec for ChatMessage serialization
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "io.quarkiverse.langchain4j.QuarkusChatMessageJsonCodecFactory",
                "io.quarkiverse.langchain4j.QuarkusChatMessageJsonCodecFactory$Codec")
                .methods()
                .constructors()
                .fields()
                .build());

        // Register the JSON codec as a service provider
        nativeResources.produce(new NativeImageResourceBuildItem(
                "META-INF/services/dev.langchain4j.data.message.ChatMessageJsonCodecFactory"));

        // Register Jackson ObjectMapper and related classes for ChatMessage deserialization
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "com.fasterxml.jackson.databind.ObjectMapper",
                "com.fasterxml.jackson.databind.DeserializationContext",
                "com.fasterxml.jackson.databind.SerializationConfig",
                "com.fasterxml.jackson.databind.DeserializationConfig")
                .methods()
                .build());

        // Register MCP-related classes if MCP client is used
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "dev.langchain4j.mcp.client.DefaultMcpClient",
                "dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport")
                .methods()
                .constructors()
                .build());

        // Register QL4J's QuarkusJsonCodecFactory mixin classes for reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$ObjectMapperHolder",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$ChatMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$SystemMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$UserMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$AiMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$ToolExecutionResultMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$CustomMessageMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$ToolExecutionRequestMixin",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$SnakeCaseObjectMapperHolder",
                "io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$SnakeCaseObjectMapperHolder$QuarkusLangChain4jModule")
                .methods()
                .fields()
                .constructors()
                .build());

        // Register QL4J internal classes that manage state during tool execution
        // Attempt to fix "messages cannot be null or empty" error when using tools in native mode
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "io.quarkiverse.langchain4j.runtime.aiservice.DefaultCommittableChatMemory",
                "io.quarkiverse.langchain4j.runtime.aiservice.CommittableChatMemory",
                "io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport",
                "io.quarkiverse.langchain4j.runtime.tool.QuarkusToolExecutor",
                "io.quarkiverse.langchain4j.runtime.tool.QuarkusToolExecutor$Context",
                "io.quarkiverse.langchain4j.runtime.tool.QuarkusToolExecutor$Wrapper",
                "dev.langchain4j.model.chat.request.ChatRequest",
                "dev.langchain4j.model.chat.request.ChatRequest$Builder",
                "dev.langchain4j.model.chat.request.DefaultChatRequestParameters",
                "dev.langchain4j.model.chat.request.DefaultChatRequestParameters$Builder")
                .methods()
                .fields()
                .constructors()
                .build());
    }
}
