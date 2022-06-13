package org.apache.camel.quarkus.test;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CamelQuarkusTestSupport extends CamelTestSupport
        implements QuarkusTestProfile {

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

    void setCurrentTestName(String currentTestName) {
        this.currentTestName = currentTestName;
    }

    void setGlobalStore(ExtensionContext.Store store) {
        this.globalStore = store;
    }
}
