package org.apache.camel.quarkus.test;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.junit.jupiter.api.TestInstance;

public class CallbakUtil {

    static boolean isPerClass(CamelQuarkusTestSupport testInstance) {
        return testInstance.getClass().getAnnotation(TestInstance.class) != null
                && testInstance.getClass().getAnnotation(TestInstance.class).value() == TestInstance.Lifecycle.PER_CLASS;
    }

    static void resetContext(CamelQuarkusTestSupport testInstance) {
        ((FastCamelContext) testInstance.context).reset();

        MockEndpoint.resetMocks(testInstance.context);
    }

}
