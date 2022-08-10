package org.apache.camel.quarkus.test.support.google;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If annotated field's name is equal to any property from the @{link
 * {@link org.apache.camel.quarkus.test.support.google.GoogleCloudContext},
 * field is injected by the property value (uses @{link
 * {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector}}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GoogleProperty {

    /**
     * @return Name of the property to inject its value into the field.
     */
    String name();
}
