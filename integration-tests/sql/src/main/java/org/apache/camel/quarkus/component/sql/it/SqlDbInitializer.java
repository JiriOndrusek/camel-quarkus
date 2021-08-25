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

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SqlDbInitializer {

    @ConfigProperty(name = "quarkus.camel.sql.script-files")
    List<String> initScripts;

    @Inject
    @DataSource("camel-sql")
    AgroalDataSource dataSource;

    public void initDb() throws SQLException, IOException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {

                List<String> scripts = initScripts.stream().filter(s -> s.contains("initDb")).collect(Collectors.toList());

                for (String script : scripts) {
                    try (InputStream is = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(script);
                            InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader reader = new BufferedReader(isr)) {

                        reader.lines().filter(s -> s != null && !"".equals(s) && !s.startsWith("--")).forEach(s -> {
                            try {
                                statement.execute(s);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
        }
    }

}
