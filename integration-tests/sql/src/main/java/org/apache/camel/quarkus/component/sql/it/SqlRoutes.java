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
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import io.agroal.api.AgroalDataSource;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.transaction.jta.JtaTransactionManager;

@ApplicationScoped
public class SqlRoutes extends RouteBuilder {

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbKind;

    @Inject
    @Named("results")
    Map<String, List> results;

    @Inject
    TransactionManager tm;

    @Inject
    UserTransaction userTransaction;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    SqlDbInitializer sqlDbInitializer;

    @Override
    public void configure() throws IOException, SQLException {
        //db has to be initialized before routes are started
        sqlDbInitializer.initDb();

        String representationOfTrue = SqlHelper.convertBooleanToSqlDialect(dbKind, true);
        String representationOfFalse = SqlHelper.convertBooleanToSqlDialect(dbKind, false);

        from("sql:select * from projectsViaSql where processed = " + representationOfFalse
                + " order by id?initialDelay=0&delay=50&consumer.onConsume=update projectsViaSql set processed = "
                + representationOfTrue + " where id = :#id")
                        .process(e -> results.get("consumerRoute").add(e.getMessage().getBody(Map.class)));

        from("sql:classpath:sql/" + dbKind
                + "/selectProjects.sql?initialDelay=0&delay=50&consumer.onConsume=update projectsViaClasspath set processed = "
                + representationOfTrue)
                        .process(e -> results.get("consumerClasspathRoute").add(e.getMessage().getBody(Map.class)));

        Path tmpFile = createTmpFileFrom("sql/" + dbKind + "/selectProjects.sql");
        from("sql:file:" + tmpFile
                + "?initialDelay=0&delay=50&consumer.onConsume=update projectsViaFile set processed = " + representationOfTrue)
                        .process(e -> results.get("consumerFileRoute").add(e.getMessage().getBody(Map.class)));

        from("direct:transacted")
                .transacted("PROPAGATION_REQUIRED")
                .to("sql:overriddenByTheHeader")
                .process(e -> {
                    if (e.getIn().getHeader("rollback", boolean.class)) {
                        throw new Exception("forced Exception");
                    }
                });

        // Idempotent Repository
        JdbcMessageIdRepository repo = new JdbcMessageIdRepository(dataSource, "idempotentRepo");
        from("direct:idempotent")
                .idempotentConsumer(header("messageId"), repo)
                .process(e -> results.get("idempotentRoute").add(e.getMessage().getBody(String.class)));

        //aggregation repository
        JdbcAggregationRepository aggregationRepo = new JdbcAggregationRepository(
                new JtaTransactionManager(userTransaction, tm), "aggregation", dataSource);
        from("direct:aggregation")
                .aggregate(header("messageId"), new MyAggregationStrategy())
                // use our created jdbc repo as aggregation repository
                .completionSize(4).aggregationRepository(aggregationRepo)
                .process(e -> results.get("aggregationRoute").add(e.getMessage().getBody(String.class)));

    }

    private Path createTmpFileFrom(String file) throws IOException {
        File tmpFile = File.createTempFile("selectProjects-", ".sql");
        tmpFile.deleteOnExit();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                FileOutputStream fos = new FileOutputStream(tmpFile)) {

            int c;
            while ((c = is.read()) >= 0) {
                baos.write(c);
            }
            String content = new String(baos.toByteArray());
            content = content.replaceAll("projectsViaClasspath", "projectsViaFile");
            fos.write(content.getBytes());
        }
        return tmpFile.toPath();
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List> results() {
        Map<String, List> result = new HashMap<>();
        result.put("consumerRoute", new CopyOnWriteArrayList<>());
        result.put("consumerClasspathRoute", new CopyOnWriteArrayList<>());
        result.put("consumerFileRoute", new CopyOnWriteArrayList<>());
        result.put("idempotentRoute", new CopyOnWriteArrayList<>());
        result.put("aggregationRoute", new CopyOnWriteArrayList<>());
        return result;
    }

    static class MyAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            String body1 = oldExchange.getIn().getBody(String.class);
            String body2 = newExchange.getIn().getBody(String.class);

            oldExchange.getIn().setBody(body1 + body2);
            return oldExchange;
        }
    }
}
