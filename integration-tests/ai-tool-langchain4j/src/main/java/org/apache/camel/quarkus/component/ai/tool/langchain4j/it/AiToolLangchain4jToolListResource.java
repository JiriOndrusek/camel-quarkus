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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it;

import java.util.stream.Collectors;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolProviderRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.component.support.langchain4j.CamelToolProvider;

@Path("/ai-tool-langchain4j")
@ApplicationScoped
public class AiToolLangchain4jToolListResource {

    @Inject
    CamelToolProvider camelToolProvider;

    @Path("/tools")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listTools() {
        return camelToolProvider.provideTools(new ToolProviderRequest(null, UserMessage.from("list")))
                .tools().keySet().stream()
                .map(spec -> spec.name())
                .sorted()
                .collect(Collectors.joining(","));
    }
}
