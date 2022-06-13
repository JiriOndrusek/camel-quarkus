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

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.Service;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CamelQuarkusTestSupport extends CamelTestSupport
        implements QuarkusTestProfile {

    private boolean initialized;

    //Flag, whether routes was created by test's route builder and therefore should be stopped smd removed based on lifecycle
    private boolean wasUsedRouteBuilder;

    @Inject
    protected CamelContext context;

    @Override
    protected CamelContext createCamelContext() throws Exception {
        return this.context;
    }

    @Override
    protected Registry createCamelRegistry() {
        throw new UnsupportedOperationException("won't be executed.");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        //replaced by quarkus callback (beforeEach)
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        //replaced by quarkus callback (beforeEach)
    }

    @Override
    public void afterAll(ExtensionContext context) {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
        try {
            doPostTearDown();
            cleanupResources();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
    }

    @Override
    protected void stopCamelContext() throws Exception {
        //context is started and stopped via quarkus lifecycle
    }

    @Override
    protected void doQuarkusCheck() {
        //can run on Quarkus
    }

    public void mockBeforeAll(ExtensionContext context) {
        super.beforeAll(context);
    }

    public void mockBeforeEach(ExtensionContext context) throws Exception {
        super.beforeEach(context);
    }

    @Override
    public boolean isUseRouteBuilder() {
        if (context.getRoutes().isEmpty()) {
            wasUsedRouteBuilder = super.isUseRouteBuilder();
            return wasUsedRouteBuilder;
        }

        return false;
    }

    @Override
    protected void doSetUp() throws Exception {
        context.getManagementStrategy();
        if (!initialized) {
            super.doSetUp();
            initialized = true;
        }
    }

    @Override
    protected void doPreSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).suspend();
        }
        super.doPreSetup();
    }

    @Override
    protected void doPostSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).resume();
            if (isUseDebugger()) {
                ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
                List<RouteDefinition> rdfs = mcc.getRouteDefinitions();
                //addition causes removal -> it starts routes, which were added during setUp, when context was suspended
                mcc.addRouteDefinitions(rdfs);
            }
        }
        super.doPostSetup();
    }

    RoutesBuilder getRouteBuilder() throws Exception {
        return createRouteBuilder();
    }

    @Override
    protected void doStopCamelContext(CamelContext context, Service camelContextService) {
        //don't stop
    }

    @Override
    protected void startCamelContext() {
        //context has already started
    }

    boolean isWasUsedRouteBuilder() {
        return wasUsedRouteBuilder;
    }
}
