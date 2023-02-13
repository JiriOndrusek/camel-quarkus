package com.apache.camel.quarkus.junit5.modules.doubleRouteBuilder;

import org.apache.camel.builder.RouteBuilder;

public class RouteConfig extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        getCamelContext().setTracing(true);

        from("direct:start").routeId("SampleRoute").setBody(constant("Some Value")).log("The body is: ${body}");

        from("timer:timeToAct?period=5000").routeId("TimerRoute").log("Calling direct:start").to("direct:start");
    }
}
