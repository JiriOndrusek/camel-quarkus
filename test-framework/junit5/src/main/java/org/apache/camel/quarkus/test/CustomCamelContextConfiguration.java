package org.apache.camel.quarkus.test;

import org.apache.camel.test.junit5.CamelContextConfiguration;

public final class CustomCamelContextConfiguration extends CamelContextConfiguration {

    CustomCamelContextConfiguration withCustomCamelContextSupplier(CamelContextSupplier camelContextSupplier) {
        return (CustomCamelContextConfiguration)super.withCamelContextSupplier(camelContextSupplier);
    }

    CustomCamelContextConfiguration withCustomPostProcessor(PostProcessor postProcessor) {
        return (CustomCamelContextConfiguration)super.withPostProcessor(postProcessor);
    }

    CustomCamelContextConfiguration withCustomRoutesSupplier(RoutesSupplier routesSupplier) {
        return (CustomCamelContextConfiguration) super.withRoutesSupplier(routesSupplier);
    }
}
