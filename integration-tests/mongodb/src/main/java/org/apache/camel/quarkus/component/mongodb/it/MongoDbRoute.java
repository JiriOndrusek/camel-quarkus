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

    public static String COLLECTION_TAILING = "tailingCollection";
    public static String COLLECTION_PERSISTENT_TAILING = "persistentTailingCollection";
    public static String COLLECTION_STREAM_CHANGES = "streamChangesgCollection";

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @Override
    public void configure() {
        from("mongodb:" + MongoDbResource.DEFAULT_MONGO_CLIENT_NAME
                + "?database=test&collection=" + COLLECTION_TAILING + "&tailTrackIncreasingField=increasing")
                        .process(e -> results.get(COLLECTION_TAILING).add(e.getMessage().getBody(Document.class)));

        from("mongodb:" + MongoDbResource.DEFAULT_MONGO_CLIENT_NAME
                + "?database=test&collection=" + COLLECTION_PERSISTENT_TAILING
                + "&tailTrackIncreasingField=increasing&persistentTailTracking=true&persistentId=darwin\"")
                        .id(COLLECTION_PERSISTENT_TAILING)
                        .process(e -> results.get(COLLECTION_PERSISTENT_TAILING).add(e.getMessage().getBody(Document.class)));

        from("mongodb:" + MongoDbResource.DEFAULT_MONGO_CLIENT_NAME
                + "?database=test&collection=" + COLLECTION_STREAM_CHANGES
                + "&consumerType=changeStreams")
                        //                        .routeProperty("streamFilter", "{'$match':{'$or':[{'fullDocument.string': 'value2'}]}}")
                        .process(e -> results.get(COLLECTION_STREAM_CHANGES).add(e.getMessage().getBody(Document.class)));
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<Document>> results() {
        Map<String, List<Document>> result = new HashMap<>();
        result.put(COLLECTION_TAILING, new CopyOnWriteArrayList<>());
        result.put(COLLECTION_PERSISTENT_TAILING, new CopyOnWriteArrayList<>());
        result.put(COLLECTION_STREAM_CHANGES, new CopyOnWriteArrayList<>());
        return result;
    }
}
