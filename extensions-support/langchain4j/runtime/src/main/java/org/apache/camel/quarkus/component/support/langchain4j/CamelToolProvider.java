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
package org.apache.camel.quarkus.component.support.langchain4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.ai.tool.AiToolExecutor;
import org.apache.camel.component.ai.tool.AiToolParameterHelper;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolResult;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.support.DefaultExchange;
import org.jboss.logging.Logger;

/**
 * Bridges Camel's {@link AiToolRegistry} to langchain4j's {@link ToolProvider} SPI. When registered as a CDI bean (done
 * automatically by the deployment processor when both {@code camel-ai-tool} and quarkus-langchain4j are on the
 * classpath), all Camel routes registered via {@code ai-tool:} endpoints become available to
 * {@code @RegisterAiService} AI services without explicit configuration.
 */
public class CamelToolProvider implements ToolProvider {

    private static final Logger LOG = Logger.getLogger(CamelToolProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final Map<String, String> TAG_MAP = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_TAG = new ThreadLocal<>();

    @Inject
    CamelContext camelContext;

    static void setCurrentTag(String tag) {
        CURRENT_TAG.set(tag);
    }

    static void clearCurrentTag() {
        CURRENT_TAG.remove();
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        String effectiveTag = CURRENT_TAG.get();
        Set<AiToolSpec> tools = effectiveTag != null ? registry.getToolsByTag(effectiveTag) : registry.getAllTools();

        ToolProviderResult.Builder resultBuilder = ToolProviderResult.builder();
        for (AiToolSpec spec : tools) {
            ToolSpecification toolSpec = toToolSpecification(spec);
            ToolExecutor executor = createExecutor(spec);
            resultBuilder.add(toolSpec, executor);
        }

        return resultBuilder.build();
    }

    private ToolExecutor createExecutor(AiToolSpec spec) {
        return (ToolExecutionRequest request, Object memoryId) -> {
            Map<String, Object> arguments = parseArguments(request);
            if (arguments == null) {
                return "Invalid arguments: could not parse the provided JSON arguments: " + request.arguments();
            }
            Exchange exchange = new DefaultExchange(camelContext);
            AiToolResult result = AiToolExecutor.execute(spec, arguments, exchange);
            return toToolResponse(spec.getName(), result);
        };
    }

    private Map<String, Object> parseArguments(ToolExecutionRequest request) {
        String jsonArguments = request.arguments();
        if (jsonArguments == null || jsonArguments.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(jsonArguments, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.debugf(e, "Failed to parse tool arguments: %s", jsonArguments);
            return null;
        }
    }

    private String toToolResponse(String toolName, AiToolResult result) {
        if (result instanceof AiToolResult.Success success) {
            return success.value();
        } else if (result instanceof AiToolResult.ArgumentError error) {
            return "Invalid arguments: " + error.message();
        } else if (result instanceof AiToolResult.ExecutionError error) {
            LOG.warnf("Tool '%s' execution failed: %s", toolName, error.message());
            return "Tool execution failed";
        }
        return "Tool execution failed";
    }

    static ToolSpecification toToolSpecification(AiToolSpec spec) {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(spec.getName())
                .description(spec.getDescription());

        if (spec.getParameterDefs() != null && !spec.getParameterDefs().isEmpty()) {
            builder.parameters(buildSchema(spec.getParameterDefs()));
        }

        return builder.build();
    }

    private static JsonObjectSchema buildSchema(Map<String, AiToolParameterHelper.ParameterDef> defs) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        List<String> required = new ArrayList<>();

        for (Map.Entry<String, AiToolParameterHelper.ParameterDef> entry : defs.entrySet()) {
            String paramName = entry.getKey();
            AiToolParameterHelper.ParameterDef def = entry.getValue();

            JsonSchemaElement schema;
            if (def.getEnumValues() != null && !def.getEnumValues().isEmpty()) {
                schema = JsonEnumSchema.builder()
                        .enumValues(def.getEnumValues())
                        .description(def.getDescription())
                        .build();
            } else {
                schema = switch (def.getType().toLowerCase(Locale.ROOT)) {
                    case "integer", "int", "long" -> JsonIntegerSchema.builder().description(def.getDescription()).build();
                    case "number", "double", "float" -> JsonNumberSchema.builder().description(def.getDescription()).build();
                    case "boolean", "bool" -> JsonBooleanSchema.builder().description(def.getDescription()).build();
                    default -> JsonStringSchema.builder().description(def.getDescription()).build();
                };
            }

            schemaBuilder.addProperty(paramName, schema);
            if (def.isRequired()) {
                required.add(paramName);
            }
        }

        if (!required.isEmpty()) {
            schemaBuilder.required(required);
        }

        return schemaBuilder.build();
    }
}
