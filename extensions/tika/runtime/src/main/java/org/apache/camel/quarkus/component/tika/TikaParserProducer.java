package org.apache.camel.quarkus.component.tika;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class TikaParserProducer {

    private volatile TikaParser parser;

    void initialize(TikaParser parser) {
        this.parser = parser;
    }

    @Singleton
    @Produces
    public TikaParser tikaParser() {
        return parser;
    }
}
