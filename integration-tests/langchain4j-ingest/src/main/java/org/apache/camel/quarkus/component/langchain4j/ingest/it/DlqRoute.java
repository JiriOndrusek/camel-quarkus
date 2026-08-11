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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

/** Captures exchanges the 'custom' pipeline's Dead Letter Channel routed away. */
@ApplicationScoped
public class DlqRoute extends RouteBuilder {

    static final List<String> DEAD_LETTERS = new CopyOnWriteArrayList<>();

    @Override
    public void configure() {
        from("direct:custom-dlq")
                .routeId("custom-dlq")
                .process(exchange -> DEAD_LETTERS.add(String.valueOf(exchange.getMessage().getBody(String.class))));
    }
}
