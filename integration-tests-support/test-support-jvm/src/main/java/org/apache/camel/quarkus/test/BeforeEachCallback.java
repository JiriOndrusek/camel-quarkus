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
    }

    private String getDisplayName(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append("(");
        sb.append(Arrays.stream(method.getParameterTypes()).map(c -> c.getSimpleName()).collect(Collectors.joining(", ")));
        sb.append(")");

        return sb.toString();
    }
}
