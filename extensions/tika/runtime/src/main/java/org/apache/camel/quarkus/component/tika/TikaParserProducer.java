package org.apache.camel.quarkus.component.tika;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.tika.config.TikaConfig;

@ApplicationScoped
public class TikaParserProducer {

    private volatile org.apache.tika.config.TikaConfig config;

    public TikaParserProducer(TikaConfig config) {
        this.config = config;
    }

    public org.apache.tika.config.TikaConfig getConfig() {
        return config;
    }
}
