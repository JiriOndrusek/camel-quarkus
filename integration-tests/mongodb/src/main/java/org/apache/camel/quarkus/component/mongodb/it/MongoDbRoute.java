package org.apache.camel.quarkus.component.mongodb.it;

import org.apache.camel.builder.RouteBuilder;

public class MongoDbRoute extends RouteBuilder {


    @Override
    public void configure() {
        from("mongodb:" + MongoDbResource.DEFAULT_MONGO_CLIENT_NAME + "?database=test&collection=cappedCollection&tailTrackIncreasingField=increasing")
                .id("tailing").autoStartup(false).log("${body}");
    }
}
