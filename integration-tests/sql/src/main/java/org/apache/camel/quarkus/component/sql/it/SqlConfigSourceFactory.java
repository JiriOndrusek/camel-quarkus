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
        String dbKind = System.getProperty("cq.sqlJdbcKind");

        Map<String, String> props = new HashMap();

        if (SqlHelper.isDerbyInDocker()) {
            //in he first call, ConfigProvider.getConfig() throws NPE - should be ignored
            try {
                Integer port = ConfigProvider.getConfig().getValue("camel.sql.derby.port", Integer.class);
                jdbcUrl = "jdbc:derby://localhost:" + port + "/DOCKERDB;create=true";
            } catch (NullPointerException e) {
                //ignore NPE
            }
        }

        //external db
        if (jdbcUrl != null) {
            props.put("quarkus.datasource.jdbc.url", jdbcUrl);
            props.put("quarkus.datasource.username", System.getenv("SQL_JDBC_USERNAME"));
            props.put("quarkus.datasource.password", System.getenv("SQL_JDBC_PASSWORD"));
        }

        if (SqlHelper.isDerbyInDocker()) {
            props.put("quarkus.native.resources.includes", "sql/derby/*.sql,sql/derby_docker/*.sql,sql/common/*.sql");
        } else {
            props.put("quarkus.native.resources.includes", "sql/" + dbKind + "/*.sql,sql/common/*.sql");
        }
        props.put("quarkus.devservices.enabled", SqlHelper.shouldStartDevService() + "");

        System.out.println("**********************************************");
        System.out.println(props);
        System.out.println("**********************************************");

        source = new MapBackedConfigSource("env_database", props) {
        };
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(source);
    }
}
