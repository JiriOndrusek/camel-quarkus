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

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that sets the current Camel AI tool tag on a ThreadLocal before an AI service method executes.
 * This allows {@link CamelToolProvider#provideTools} to filter tools by the tag associated with the calling AI service,
 * enabling multiple {@code @RegisterAiService} interfaces with different {@code @CamelTools} tags in the same
 * application.
 */
@Interceptor
@CamelToolsBinding
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class CamelToolsInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Class<?> targetClass = ctx.getTarget().getClass();
        String tag = resolveTag(targetClass);
        if (tag != null) {
            CamelToolProvider.setCurrentTag(tag);
        }
        try {
            return ctx.proceed();
        } finally {
            CamelToolProvider.clearCurrentTag();
        }
    }

    private String resolveTag(Class<?> targetClass) {
        // Walk the class hierarchy and interfaces to find the tag mapping.
        // The generated $$QuarkusImpl class implements the @RegisterAiService interface,
        // so we check the class itself and all its interfaces against the tag map.
        String tag = CamelToolProvider.TAG_MAP.get(targetClass.getName());
        if (tag != null) {
            return tag;
        }
        for (Class<?> iface : targetClass.getInterfaces()) {
            tag = CamelToolProvider.TAG_MAP.get(iface.getName());
            if (tag != null) {
                return tag;
            }
        }
        return null;
    }
}
