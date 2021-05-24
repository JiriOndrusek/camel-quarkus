package org.apache.camel.quarkus.component.sql.it;

import org.apache.camel.builder.RouteBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SqlRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Map>> results;

    @Override
    public void configure() {
        from("sql:select * from projects where processed = false order by id?initialDelay=0&delay=50&consumer.onConsume=update projects set processed = true where id = :#id")
                .id("consumerRoute").autoStartup(false)
                .process(e -> {
                    System.out.println("************8 received ");
                    results.get("consumerResults").add(e.getMessage().getBody(Map.class));
                });

    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<Map>> results() {
        Map<String, List<Map>> result = new HashMap<>();
        result.put("consumerResults", new CopyOnWriteArrayList<>());
        return result;
    }
}
