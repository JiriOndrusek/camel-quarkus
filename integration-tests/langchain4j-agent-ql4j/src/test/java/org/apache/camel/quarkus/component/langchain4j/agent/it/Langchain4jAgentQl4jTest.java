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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.Matchers.*;

@ExtendWith(Langchain4jTestWatcher.class)
@QuarkusTestResource(Langchain4jAgentTestResource.class)
@QuarkusTest
class Langchain4jAgentQl4jTest {
    @Test
    void simpleUserMessage() {
        RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(200)
                .body(
                        not(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE),
                        containsString("Apache Camel"));
    }

    /**
     * Verifies that AgentWithoutMemory is truly stateless when Quarkus LangChain4j is on the classpath
     * and a default model is configured. Calls the agent twice within a single HTTP request; if QL4J's
     * ChatMemoryProvider leaks in, the second call's LLM request will include the first call's history,
     * causing WireMock to fail matching (the request body will differ from the expected single-message payload).
     *
     * @see <a href="https://github.com/apache/camel-quarkus/issues/8836">#8836</a>
     */
    @Test
    void agentWithoutMemoryIsStateless() {
        RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post("/langchain4j-agent-ql4j/stateless-check")
                .then()
                .statusCode(200)
                .body("stateless", is(true));
    }
}
