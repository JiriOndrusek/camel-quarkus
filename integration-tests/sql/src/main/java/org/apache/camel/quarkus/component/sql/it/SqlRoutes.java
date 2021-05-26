package org.apache.camel.quarkus.component.sql.it;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class SqlRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Map>> results;

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

        File tmpFile = File.createTempFile("selectProjects-", ".sql");
        tmpFile.deleteOnExit();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sql/selectProjects.sql")) { //todo quick workaround

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) >= 0) {
                out.write(c);
            }
            new FileOutputStream(tmpFile).write(out.toByteArray());
        }

        from("sql:file:" + tmpFile.toPath()
                + "?initialDelay=0&delay=50&consumer.onConsume=update projects set processed = true")
                        .id("consumerFileRoute").autoStartup(false)
                        .process(e -> {
                            System.out.println("received ++++++");
                            results.get("consumerFileRoute").add(e.getMessage().getBody(Map.class));
                        });

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
