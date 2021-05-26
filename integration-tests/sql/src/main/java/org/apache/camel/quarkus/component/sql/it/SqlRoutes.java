package org.apache.camel.quarkus.component.sql.it;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.TransactionManager;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class SqlRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Map>> results;

    @Inject
    TransactionManager tm;

    @Override
    public void configure() throws IOException {

        from("sql:select * from projects where processed = false order by id?initialDelay=0&delay=50&consumer.onConsume=update projects set processed = true where id = :#id")
                .id("consumerRoute").autoStartup(false)
                .process(e -> {
                    results.get("consumerRoute").add(e.getMessage().getBody(Map.class));
                });

        from("sql:classpath:sql/selectProjects.sql?initialDelay=0&delay=50&consumer.onConsume=update projects set processed = true")
                .id("consumerClasspathRoute").autoStartup(false)
                .process(e -> {
                    System.out.println("received ++++++");
                    results.get("consumerClasspathRoute").add(e.getMessage().getBody(Map.class));
                });

        Path tmpFile = createTmpFileFrom("sql/selectProjects.sql");

        from("sql:file:" + tmpFile
                + "?initialDelay=0&delay=50&consumer.onConsume=update projects set processed = true")
                        .id("consumerFileRoute").autoStartup(false)
                        .process(e -> {
                            System.out.println("received ++++++");
                            results.get("consumerFileRoute").add(e.getMessage().getBody(Map.class));
                        });

        from("direct:transacted")
                .transacted("PROPAGATION_REQUIRED")
                .to("sql:overriddenByTheHeader")
                .process(e -> {
                    if (e.getIn().getHeader("rollback", boolean.class)) {
                        throw new Exception("forced Exception");
                    }
                });

    }

    private Path createTmpFileFrom(String file) throws IOException {
        File tmpFile = File.createTempFile("selectProjects-", ".sql");
        tmpFile.deleteOnExit();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) { //todo quick workaround

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) >= 0) {
                out.write(c);
            }
            new FileOutputStream(tmpFile).write(out.toByteArray());
        }
        return tmpFile.toPath();
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<Map>> results() {
        Map<String, List<Map>> result = new HashMap<>();
        result.put("consumerRoute", new CopyOnWriteArrayList<>());
        result.put("consumerClasspathRoute", new CopyOnWriteArrayList<>());
        result.put("consumerFileRoute", new CopyOnWriteArrayList<>());
        return result;
    }
}
