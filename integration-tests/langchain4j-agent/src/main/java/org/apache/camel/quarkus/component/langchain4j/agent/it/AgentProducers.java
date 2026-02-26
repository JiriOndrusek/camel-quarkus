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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkiverse.langchain4j.ModelName;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import org.apache.camel.component.langchain4j.agent.api.AgentWithoutMemory;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.TestPojoJsonExtractorOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.service.TestPojoAiAgent;
import org.apache.camel.quarkus.component.langchain4j.agent.it.tool.AdditionTool;
import org.apache.camel.quarkus.component.langchain4j.agent.it.util.PersistentChatMemoryStore;
import org.apache.camel.quarkus.component.langchain4j.agent.it.util.ProcessUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static java.time.Duration.ofSeconds;

@ApplicationScoped
public class AgentProducers {
    @ConfigProperty(name = "langchain4j.ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "nodejs.installed")
    boolean isNodeJSInstaled;

    @Inject
    @ModelName("ollamaOrcaMiniModel")
    ChatModel ollamaOrcaMiniModel;

    @Inject
    @ModelName("granite4Model")
    ChatModel granite4Model;

    @Inject
    @ModelName("nomicEmbed")
    EmbeddingModel nomicEmbed;
    //
    //    @Produces
    //    @Identifier("ollamaOrcaMiniModel")
    //    ChatModel ollamaOrcaMiniModel() {
    //        return OllamaChatModel.builder()
    //                .baseUrl(baseUrl)
    //                .modelName("orca-mini")
    //                .temperature(0.3)
    //                .httpClientBuilder(new JaxRsHttpClientBuilder())
    ////                .httpClientBuilder(new dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory())

    //                .build();
    //    }
    //
    //    @Produces
    //    @Identifier("granite4Model")
    //    ChatModel granite4Model() {
    //        return OllamaChatModel.builder()
    //                .baseUrl(baseUrl)
    //                .modelName("granite4:3b")
    //                .temperature(0.3)
    //                .logResponses(true)
    //                .logRequests(true)
    //                .httpClientBuilder(new JaxRsHttpClientBuilder())
    ////                .httpClientBuilder(new dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory())
    //                .build();
    //    }

    @Produces
    ChatMemoryStore chatMemoryStore() {
        return new PersistentChatMemoryStore();
    }

    //    @Produces
    RetrievalAugmentor retrievalAugmentor() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream("rag/company-knowledge-base.txt")) {
            if (stream == null) {
                throw new IllegalArgumentException("company-knowledge.txt not found");
            }

            Document document = Document.from(new String(stream.readAllBytes(), StandardCharsets.UTF_8));

            List<TextSegment> segments = DocumentSplitters.recursive(300, 100).split(document);

            List<Embedding> embeddings = nomicEmbed.embedAll(segments).content();

            // Store in embedding store
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            embeddingStore.addAll(embeddings, segments);

            // Create content retriever
            EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(nomicEmbed)
                    .maxResults(3)
                    .minScore(0.6)
                    .build();

            // Create a RetrievalAugmentor that uses only a content retriever : naive rag scenario
            return DefaultRetrievalAugmentor.builder()
                    .contentRetriever(contentRetriever)
                    .build();
        }
    }

    @Produces
    @Identifier("simpleAgent")
    Agent simpleAgent() {
        return new AgentWithoutMemory(new AgentConfiguration().withChatModel(ollamaOrcaMiniModel));
    }

    @Produces
    @Identifier("agentWithMemory")
    Agent agentWithMemory(ChatMemoryStore chatMemoryStore) {
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore)
                .build();

        return new AgentWithMemory(new AgentConfiguration()
                .withChatModel(granite4Model)
                .withChatMemoryProvider(chatMemoryProvider));
    }

    @Produces
    @Identifier("agentWithSuccessInputGuardrail")
    Agent agentWithSuccessInputGuardrail() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withInputGuardrailClasses(List.of(ValidationSuccessInputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithFailingInputGuardrail")
    Agent agentWithFailingInputGuardrail() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withInputGuardrailClasses(List.of(ValidationFailureInputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithSuccessOutputGuardrail")
    Agent agentWithSuccessOutputGuardrail() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withOutputGuardrailClasses(List.of(ValidationSuccessOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithFailingOutputGuardrail")
    Agent agentWithFailingOutputGuardrail() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withOutputGuardrailClasses(List.of(ValidationFailureOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithJsonExtractorOutputGuardrail")
    Agent agentWithJsonExtractorOutputGuardrail() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withOutputGuardrailClasses(List.of(TestPojoJsonExtractorOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithRag")
    public Agent agentWithRag() throws IOException {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel)
                .withRetrievalAugmentor(retrievalAugmentor()));
    }

    @Produces
    @Identifier("agentWithTools")
    public Agent agentWithTools() {
        return new AgentWithoutMemory(new AgentConfiguration().withChatModel(granite4Model));
    }

    @Produces
    @Identifier("agentWithCustomService")
    public Agent agentCustom(ObjectMapper objectMapper) {
        return new TestPojoAiAgent(new AgentConfiguration()
                .withChatModel(ollamaOrcaMiniModel), objectMapper);
    }

    @Produces
    @Identifier("agentWithCustomTools")
    Agent agentWithCustomTools() {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(granite4Model)
                .withCustomTools(List.of(new AdditionTool())));
    }

    @Produces
    @Identifier("agentWithMcpClient")
    Agent agentWithMcpClient() {
        if (isNodeJSInstaled) {
            return new AgentWithoutMemory(new AgentConfiguration()
                    .withChatModel(granite4Model)
                    .withMcpClient(new DefaultMcpClient.Builder()
                            // Startup on Windows is really slow (around 1min).
                            .initializationTimeout(Duration.ofSeconds(120))
                            .transport(new StdioMcpTransport.Builder()
                                    .command(List.of(ProcessUtils.getNpxExecutable(), "-y",
                                            "@modelcontextprotocol/server-everything@2025.12.18"))
                                    .logEvents(true)
                                    .build())
                            .build())
                    .withMcpToolProviderFilter((mcpClient, toolSpecification) -> {
                        String toolName = toolSpecification.name().toLowerCase();
                        return toolName.contains("add") || toolName.contains("echo") || toolName.contains("long");
                    }));
        }
        return null;
    }
}
