package org.apache.camel.quarkus.component.langchain4j.agent.deployment;

import java.util.function.BooleanSupplier;

public class QuarkusLangchain4jPresent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("io.quarkiverse.langchain4j.RegisterAiService");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
