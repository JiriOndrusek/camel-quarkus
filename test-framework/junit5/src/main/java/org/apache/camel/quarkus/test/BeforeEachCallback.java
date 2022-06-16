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
package org.apache.camel.quarkus.test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.ExtensionValuesStore;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;

public class BeforeEachCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        CamelQuarkusTestSupport testInstance = (CamelQuarkusTestSupport) context.getTestInstance();

        testInstance.setCurrentTestName(getDisplayName(context.getTestMethod()));
        testInstance.setGlobalStore(new NamespaceAwareStore(new ExtensionValuesStore(null), ExtensionContext.Namespace.GLOBAL));

        testInstance.setIsCreateCamelContextPerClass(CallbakUtil.isPerClass(testInstance));
    }

    private String getDisplayName(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append("(");
        sb.append(Arrays.stream(method.getParameterTypes()).map(c -> c.getSimpleName()).collect(Collectors.joining(", ")));
        sb.append(")");

        return sb.toString();
    }
}
