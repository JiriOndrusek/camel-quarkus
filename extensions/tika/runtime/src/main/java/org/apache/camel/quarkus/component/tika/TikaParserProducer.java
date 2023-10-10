package org.apache.camel.quarkus.component.tika;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;

@ApplicationScoped
public class TikaParserProducer {

    private volatile String xmlConfig;

    void initialize(String xmlConfig) {
        this.xmlConfig = xmlConfig;
    }

    @Singleton
    @Produces
    public Supplier<Parser> tikaParser() {
        return () -> {
            TikaConfig tikaConfig;
            try (InputStream stream = new ByteArrayInputStream(xmlConfig.getBytes(StandardCharsets.UTF_8))) {
                tikaConfig = new TikaConfig(stream);
                return new AutoDetectParser(tikaConfig);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
