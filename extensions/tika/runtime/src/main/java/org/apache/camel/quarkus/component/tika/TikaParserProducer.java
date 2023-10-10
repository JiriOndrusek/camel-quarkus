package org.apache.camel.quarkus.component.tika;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.tika.parser.Parser;

@ApplicationScoped
public class TikaParserProducer {

    private volatile Parser parser;

    void initialize(Parser parser) {
        this.parser = parser;
    }

    @Singleton
    @Produces
    public Parser tikaParser() {
        return parser;
    }
}
