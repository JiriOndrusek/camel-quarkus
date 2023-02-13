package org.apache.camel.quarkus.test.userTestCases;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

@QuarkusTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RouteTest extends CamelQuarkusTestSupport {

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:startTest").routeId("TestRoute").to("direct:start").to("mock:result");
            }
        };
    }

    @Test
    public void someTestA() throws InterruptedException {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedBodiesReceived("Some Value");

        template.sendBody("direct:startTest", null);

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void someTestB() throws InterruptedException {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedBodiesReceived("Some Value");

        template.sendBody("direct:startTest", null);

        mockEndpoint.assertIsSatisfied();
    }
}
