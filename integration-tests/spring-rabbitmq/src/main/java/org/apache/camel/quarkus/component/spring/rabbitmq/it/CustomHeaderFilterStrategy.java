package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.util.regex.Pattern;

import org.apache.camel.support.DefaultHeaderFilterStrategy;

public class CustomHeaderFilterStrategy extends DefaultHeaderFilterStrategy {

    public CustomHeaderFilterStrategy() {
        initialize();
    }

    protected void initialize() {
        this.setOutFilterPattern(Pattern.compile("^(?!CamelSpringRabbitmqMessageId).*"));
    }

}
