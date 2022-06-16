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

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CamelQuarkusTestSupport extends CamelTestSupport
        implements QuarkusTestProfile {

    private boolean initialized = false;

    @Inject
    protected CamelContext context;

    @Override
    protected CamelContext createCamelContext() throws Exception {
        return this.context;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    public void afterAll(ExtensionContext context) {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessaryu code was moved into quarkus's callback
    }

    @Override
    protected void stopCamelContext() throws Exception {
        //context is started and stopped via quarkus lifecycle
    }

    void setCurrentTestName(String currentTestName) {
        this.currentTestName = currentTestName;
    }

    void setGlobalStore(ExtensionContext.Store store) {
        this.globalStore = store;
    }

    @Override
    public boolean isUseRouteBuilder() {
        if (context.getRoutes().isEmpty()) {
            return super.isUseRouteBuilder();
        }
        return false;
    }

    public void tearDownCreateCamelContextPerClass() throws Exception {
        super.tearDownCreateCamelContextPerClass();
    }

    //    @Override
    //    public void setUp() throws Exception {
    //        super.setUp();
    //    }

        @Override
        protected void doSetUp() throws Exception {
            if (!initialized) {
                super.doSetUp();
                initialized = true;

            }
        }

    RoutesBuilder getRouteBuilder() throws Exception {
        return createRouteBuilder();
    }

    @Override
    protected void stopTemplates() throws Exception {
        //do nothing
    }
}
