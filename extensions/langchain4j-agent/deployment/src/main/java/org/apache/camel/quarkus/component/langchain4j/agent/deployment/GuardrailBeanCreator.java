package org.apache.camel.quarkus.component.langchain4j.agent.deployment;

import dev.langchain4j.guardrail.Guardrail;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class GuardrailBeanCreator implements BeanCreator<Guardrail<?, ?>> {
    @Override
    public Guardrail<?, ?> create(SyntheticCreationalContext<Guardrail<?, ?>> context) {

        String className = (String) context.getParams().get("className");

        try {
            Class<?> clazz = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(className);

            return (Guardrail<?, ?>) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
