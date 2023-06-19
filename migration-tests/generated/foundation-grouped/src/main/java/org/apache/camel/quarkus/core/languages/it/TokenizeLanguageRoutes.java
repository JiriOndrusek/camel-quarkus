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
package org.apache.camel.quarkus.core.languages.it;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.camel.builder.RouteBuilder;

@Singleton
public class TokenizeLanguageRoutes extends RouteBuilder {

    @Inject
    @Named("tokenCounter")
    AtomicInteger tokenCounter;

    @Inject
    @Named("xmlTokenCounter")
    AtomicInteger xmlTokenCounter;

    @Override
    public void configure() {
        from("direct:tokenizeLanguage")
                .split().tokenize(",")
                .process(e -> tokenCounter.incrementAndGet());
        from("direct:tokenizeLanguageXml")
                .split().tokenizeXML("bar")
                .process(e -> xmlTokenCounter.incrementAndGet());
    }

    static class Producers {
        @Produces
        @Singleton
        @Named("tokenCounter")
        AtomicInteger tokenCounter() {
            return new AtomicInteger();
        }

        @Produces
        @Singleton
        @Named("xmlTokenCounter")
        AtomicInteger xmlTokenCounter() {
            return new AtomicInteger();
        }
    }
}
