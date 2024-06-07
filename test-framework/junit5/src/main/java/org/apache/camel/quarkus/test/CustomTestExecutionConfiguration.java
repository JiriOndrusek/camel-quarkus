package org.apache.camel.quarkus.test;

import org.apache.camel.test.junit5.TestExecutionConfiguration;

public final class CustomTestExecutionConfiguration extends TestExecutionConfiguration {

    CustomTestExecutionConfiguration withCustomUseAdviceWith(boolean useAdviceWith) {
        return (CustomTestExecutionConfiguration) super.withUseAdviceWith(useAdviceWith);
    }

    CustomTestExecutionConfiguration withCustomCreateCamelContextPerClass(boolean createCamelContextPerClass) {
        return (CustomTestExecutionConfiguration)super.withCreateCamelContextPerClass(createCamelContextPerClass);
    }
}
