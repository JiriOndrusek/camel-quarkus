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
import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;
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

        //        if (SqlHelper.isDerbyInDocker()) {
        //            //in he first call, ConfigProvider.getConfig() throws NPE - should be ignored
        //            try {
        //                System.out.println("xxxxxx - reading port 1");
        //                Integer port = ConfigProvider.getConfig().getValue("camel.sql.derby.port", Integer.class);
        //                System.out.println("xxxxxx - read port " + port);
        //                if (port != null) {
        //                    jdbcUrl = "jdbc:derby://localhost:" + port + "/DOCKERDB;create=true";
        //                }
        //            } catch (NullPointerException e) {
        //                //ignore NPE
        //            }
        //        }

        //        if (jdbcUrl == null && SqlHelper.isDerbyInDocker()) {
        //            //in he first call, ConfigProvider.getConfig() throws NPE - should be ignored
        //            try {
        //                System.out.println("xxxxxx - reading port 2");
        //                String port = System.getProperty("camel.sql.derby.port");
        //                System.out.println("xxxxxx - read port " + port);
        //                if (port != null) {
        //                    jdbcUrl = "jdbc:derby://localhost:" + port + "/DOCKERDB;create=true";
        //                }
        //            } catch (NullPointerException e) {
        //                //ignore NPE
        //            }
        //        }

        //external db
        if (jdbcUrl != null) {
            props.put("quarkus.datasource.jdbc.url", jdbcUrl);
            props.put("quarkus.datasource.username", System.getenv("SQL_JDBC_USERNAME"));
            props.put("quarkus.datasource.password", System.getenv("SQL_JDBC_PASSWORD"));
        }

        if (SqlHelper.isDerbyInDocker()) {
            props.put("quarkus.native.resources.includes", "sql/derby/*.sql,sql/derby_docker/*.sql,sql/common/*.sql");
            //            props.put("quarkus.datasource.db-kind=", dbKind);
            //            props.put("quarkus.datasource.jdbc.url", "jdbc:derby://localhost:1527/DOCKERDB;create=true");
            props.put("quarkus.datasource.jdbc.url",
                    "jdbc:derby://localhost:" + SqlHelper.getDerbyDockerPort() + "/DOCKERDB;create=true");
            //            props.put("quarkus.datasource.username", System.getenv("DOCKERDB"));
            //            props.put("quarkus.datasource.password", System.getenv("DOCKERDB"));
            //            System.out.println("==================================================================");
            //            System.out.println("==================================================================");
            //            System.out.println("====================== configuring ds        =====================");
            //            System.out.println("==================================================================");
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

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(999);
    }
}
