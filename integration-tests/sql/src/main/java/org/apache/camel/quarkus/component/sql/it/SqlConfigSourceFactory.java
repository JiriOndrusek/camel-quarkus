/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.sql.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlConfigSourceFactory implements ConfigSourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlConfigSourceFactory.class);

    private static MapBackedConfigSource source;

    static {
        String jdbcUrl = System.getenv("SQL_JDBC_URL");
        String useDerbyDocker = System.getenv("SQL_USE_DERBY_DOCKER");
        String dbKind = null;
        Map<String, String> props = new HashMap();

        try {
            dbKind = ConfigProvider.getConfig().getOptionalValue("cq.sqlJdbcKind", String.class).orElse("h2");
            if ("derby".equals(dbKind)) {
                if (Boolean.parseBoolean(useDerbyDocker)) {
                    Integer port = ConfigProvider.getConfig().getValue("camel.sql.derby.port", Integer.class);
                    jdbcUrl = "jdbc:derby://localhost:" + port + "/DOCKERDB;create=true";
                    dbKind = "derby-docker";
                }
            }

        } catch (Exception e) {
            //ConfigProvider.getConfig(... "cq.sqlJdbcKind" is not initialized yet, will be loaded in the second call
            LOGGER.debug("Can not load config properties - should be loaded in the second call", e);
        }
        //external db
        if (jdbcUrl != null) {
            props.put("quarkus.datasource.jdbc.url", jdbcUrl);
            props.put("quarkus.datasource.username", System.getenv("SQL_JDBC_USERNAME"));
            props.put("quarkus.datasource.password", System.getenv("SQL_JDBC_PASSWORD"));
            if (dbKind != null) {
                props.put("quarkus.native.resources.includes", "sql/" + dbKind + "*.sql,sql/common/*.sql");
            }
        }

        props.put("quarkus.devservices.enabled", String.valueOf(jdbcUrl != null && !Boolean.parseBoolean(useDerbyDocker)));

        source = new MapBackedConfigSource("env_database", props) {
        };
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(source);
    }
}
