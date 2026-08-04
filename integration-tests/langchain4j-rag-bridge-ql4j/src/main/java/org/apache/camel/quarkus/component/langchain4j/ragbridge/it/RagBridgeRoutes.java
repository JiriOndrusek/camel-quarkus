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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkiverse.langchain4j.EmbeddingStoreName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.embeddings.LangChain4jEmbeddingsHeaders;

@ApplicationScoped
public class RagBridgeRoutes extends RouteBuilder {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    @EmbeddingStoreName("products")
    EmbeddingStore<TextSegment> productsStore;

    @Override
    public void configure() throws Exception {
        from("direct:ingest")
                .to("langchain4j-embeddings:embed")
                .process(exchange -> {
                    Embedding embedding = exchange.getMessage()
                            .getHeader(LangChain4jEmbeddingsHeaders.VECTOR, Embedding.class);
                    String text = exchange.getMessage().getBody(String.class);
                    embeddingStore.add(embedding, TextSegment.from(text));
                });

        from("direct:ingest-products")
                .to("langchain4j-embeddings:embed")
                .process(exchange -> {
                    Embedding embedding = exchange.getMessage()
                            .getHeader(LangChain4jEmbeddingsHeaders.VECTOR, Embedding.class);
                    String text = exchange.getMessage().getBody(String.class);
                    productsStore.add(embedding, TextSegment.from(text));
                });
    }
}
