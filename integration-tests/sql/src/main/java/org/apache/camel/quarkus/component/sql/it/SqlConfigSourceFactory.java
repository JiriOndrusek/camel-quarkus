package org.apache.camel.quarkus.component.sql.it;

import java.util.Collections;
import java.util.HashMap;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class SqlConfigSourceFactory implements ConfigSourceFactory {

    private final static MapBackedConfigSource source;

    static {
        String dbKind = System.getenv("SQL_JDBC_DB_KIND");

        if (dbKind != null) {
            source = new MapBackedConfigSource("env_database", new HashMap() {
                {
                    put("quarkus.datasource.jdbc.url", System.getenv("SQL_JDBC_URL"));
                    put("quarkus.datasource.db-kind", dbKind);
                    put("quarkus.datasource.username", System.getenv("SQL_JDBC_USERNAME"));
                    put("quarkus.datasource.password", System.getenv("SQL_JDBC_PASSWORD"));
                }
            }) {
            };
        } else {
            //default source is h2
            source = new MapBackedConfigSource("env_database", new HashMap() {
                {
                    put("quarkus.datasource.db-kind", "h2");
                }
            }) {
            };
        }
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(source);
    }
}
