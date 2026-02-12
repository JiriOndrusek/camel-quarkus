package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import java.lang.reflect.InvocationTargetException;

import dev.langchain4j.guardrail.Guardrail;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

@Recorder
public class QuarkusLangchain4jRecorder {
    public void enforceJaxRsHttpClient() {
        if (System.getProperty("langchain4j.http.clientBuilderFactory") == null) {
            System.setProperty("langchain4j.http.clientBuilderFactory",
                    "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory");
        }
    }

    public RuntimeValue<Guardrail<?, ?>> instantiateGuardrails(Class<Guardrail<?, ?>> guardrailClass) {

        Class<?> cl = null;
        try {
            Object o = guardrailClass.getConstructor().newInstance();
            return o instanceof Guardrail<?, ?> ? new RuntimeValue<>((Guardrail<?, ?>) o) : null;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            Logger.getLogger(QuarkusLangchain4jRecorder.class).debugf(e,
                    "Can not instantiate guardrail of class %s", cl.getName());
            return null;
        }
    }
}
