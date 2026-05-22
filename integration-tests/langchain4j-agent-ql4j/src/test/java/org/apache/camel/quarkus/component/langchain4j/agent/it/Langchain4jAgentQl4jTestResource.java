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

import java.util.Map;

public class Langchain4jAgentQl4jTestResource extends Langchain4jAgentTestResource {
    private static final String OLLAMA_ENV_URL = "LANGCHAIN4J_OLLAMA_BASE_URL";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();

        if (server != null) {
            addGuardrailRetryStub();
        }

        return properties;
    }

    private void addGuardrailRetryStub() {
        String retryRequest = """
                {
                  "model" : "orca-mini",
                  "messages" : [ {
                    "role" : "user",
                    "content" : "Make sure you return a valid JSON object following the specified format"
                  } ],
                  "options" : {
                    "temperature" : 0.3,
                    "stop" : [ ]
                  },
                  "stream" : false,
                  "tools" : [ ]
                }""";

        String retryResponse = """
                {"model":"orca-mini","created_at":"2026-03-12T08:54:32.615262279Z","message":{"role":"assistant","content":" Sure, I'd be happy to help! Can you please provide me with the specific format you want the JSON object to follow?"},"done":true,"done_reason":"stop","total_duration":1554193662,"load_duration":17846961,"prompt_eval_count":52,"prompt_eval_duration":235272960,"eval_count":28,"eval_duration":1295301623}""";

        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/chat"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.equalToJson(retryRequest, true, false))
                .atPriority(1)
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withHeader("Date", "Thu, 12 Mar 2026 08:54:32 GMT")
                        .withBody(retryResponse)));
    }

}
