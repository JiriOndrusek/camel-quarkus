package org.apache.camel.quarkus.test;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.camel.CamelContext;
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

    @Override
    protected void doSetUp() throws Exception {
        if (!initialized) {
            super.doSetUp();
            initialized = true;

        }
    }
}
