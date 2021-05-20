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
package org.apache.camel.quarkus.component.mongodb.it;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.camel.util.CollectionHelper;
import org.bson.Document;

@Path("/mongodb")
@ApplicationScoped
public class MongoDbResource {

    static final String DEFAULT_MONGO_CLIENT_NAME = "camelMongoClient";
    static final String NAMED_MONGO_CLIENT_NAME = "myMongoClient";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @POST
    @Path("/collection/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response writeToCollection(@PathParam("collectionName") String collectionName, String content,
            @HeaderParam("mongoClientName") String mongoClientName)
            throws URISyntaxException {

        producerTemplate.sendBody(
                String.format("mongodb:%s?database=test&collection=%s&operation=insert&dynamicity=true",
                        mongoClientName, collectionName),
                content);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @GET
    @Path("/collection/{collectionName}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonArray getCollection(@PathParam("collectionName") String collectionName,
            @HeaderParam("mongoClientName") String mongoClientName) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        MongoIterable<Document> iterable = producerTemplate.requestBody(
                String.format(
                        "mongodb:%s?database=test&collection=%s&operation=findAll&dynamicity=true&outputType=MongoIterable",
                        mongoClientName, collectionName),
                null, MongoIterable.class);

        MongoCursor<Document> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            Document document = iterator.next();
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("message", (String) document.get("message"));
            arrayBuilder.add(objectBuilder.build());
        }

        return arrayBuilder.build();
    }

    @POST
    @Path("/collection/dynamic/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object dynamic(@PathParam("collectionName") String collectionName, String content,
            @HeaderParam("mongoClientName") String mongoClientName,
            @HeaderParam("dynamicOperation") String operation)
            throws URISyntaxException {

        Object result = producerTemplate.requestBodyAndHeader(
                String.format("mongodb:%s?database=test&collection=%s&operation=insert&dynamicity=true",
                        mongoClientName, collectionName),
                content, MongoDbConstants.OPERATION_HEADER, operation);

        return result;
    }

    @GET
    @Path("/restartRoute/{routeId}")
    public void restartRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
        while (camelContext.getRouteController().getRouteStatus(routeId) != ServiceStatus.Stopped) {
        }
        camelContext.getRouteController().startRoute(routeId);
        while (camelContext.getRouteController().getRouteStatus(routeId) != ServiceStatus.Started) {
        }
    }

    @GET
    @Path("/resultsReset/{resultId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map getResultsAndReset(@PathParam("resultId") String resultId) {
        int size = results.get(resultId).size();
        Document last = null;
        if (!results.get(resultId).isEmpty()) {
            last = results.get(resultId).get(size - 1);
            results.get(resultId).clear();
        }
        return CollectionHelper.mapOf("size", size, "last", last);
    }

}
