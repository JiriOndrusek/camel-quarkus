package org.apache.camel.quarkus.component.aws2.ddb;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

public class CamelJfrConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {

    private static final String PROP_PREFIX_CAMEL_JFR = "quarkus.dynamodb.aws";
    private static final String PROP_PREFIX_CAMEL_MAIN = "camel.quarkus.component.aws2";

    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext configSourceInterceptorContext) {
        return new ConfigSourceInterceptor() {
            @Override
            public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
                ConfigValue value = context.proceed(name);

                // If no quarkus.camel.jfr property value set, see if there's an equivalent camel.main property
                if (name.startsWith(PROP_PREFIX_CAMEL_JFR) && value == null) {
                    String property = name.substring(name.lastIndexOf("."));

                    ConfigValue camelMainValue = context.proceed(PROP_PREFIX_CAMEL_MAIN + property);
                    if (camelMainValue != null) {
                        return camelMainValue;
                    }

                    camelMainValue = context.proceed(PROP_PREFIX_CAMEL_MAIN + property);
                    return camelMainValue;
                }

                return value;
            }
        };
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(Priorities.LIBRARY + 1000);
    }
}
