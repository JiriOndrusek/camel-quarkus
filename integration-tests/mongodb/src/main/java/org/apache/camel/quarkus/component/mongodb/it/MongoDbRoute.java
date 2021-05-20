package org.apache.camel.quarkus.component.mongodb.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.bson.Document;

@ApplicationScoped
public class MongoDbRoute extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @Override
    public void configure() {
        from("mongodb:" + MongoDbResource.DEFAULT_MONGO_CLIENT_NAME
                + "?database=test&collection=cappedCollection&tailTrackIncreasingField=increasing")
                        .process(e -> results.get("tailing").add(e.getMessage().getBody(Document.class)));
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    public Map<String, List<Document>> results() {
        Map<String, List<Document>> result = new HashMap<>();
        result.put("tailing", new CopyOnWriteArrayList<>());
        return result;
    }
}
