package com.apache.camel.quarkus.junit5.modules.doubleRouteBuilder;

import jakarta.enterprise.inject.Produces;
import org.apache.camel.builder.RouteBuilder;

public class RouteConfig extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        getCamelContext().setTracing(true);

        from("direct:start").routeId("SampleRoute").setBody(constant("Some Value")).log("The body is: ${body}");

        from("timer:timeToAct?period=5000").routeId("TimerRoute").log("Calling direct:start").to("direct:start");
    }

    @Produces
    public RouteBuilder routes() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:in1").to("direct:in2");
            }
        };
    }
}
