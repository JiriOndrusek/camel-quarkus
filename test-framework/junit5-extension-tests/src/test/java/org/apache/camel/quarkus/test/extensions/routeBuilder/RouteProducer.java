package org.apache.camel.quarkus.test.extensions.routeBuilder;

import org.apache.camel.builder.RouteBuilder;

public class RouteProducer {

    @jakarta.enterprise.inject.Produces
    public RouteBuilder routes() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:in1").to("direct:in2");
            }
        };
    }
}
