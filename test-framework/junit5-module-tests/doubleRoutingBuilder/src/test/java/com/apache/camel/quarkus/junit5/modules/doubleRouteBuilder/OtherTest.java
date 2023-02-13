package com.apache.camel.quarkus.junit5.modules.doubleRouteBuilder;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class OtherTest {

    @Test
    public void testSomeMore() {
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("test", "10");

        int value = exchange.getProperty("test", Integer.class);

        Assertions.assertEquals(10, value, "Erroneous value");
    }
}
