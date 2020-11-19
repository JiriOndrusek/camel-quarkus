package org.apache.camel.quarkus.component.avro.rpc.it.disable;

import org.apache.camel.quarkus.component.avro.rpc.it.AvroRpcNettyTest;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableNettyCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getTestMethod().get().getDeclaringClass().equals(AvroRpcNettyTest.class)) {
            return ConditionEvaluationResult.disabled("Test disabled for netty");
        } else {
            return ConditionEvaluationResult.enabled("Test enabled");
        }
    }
}
