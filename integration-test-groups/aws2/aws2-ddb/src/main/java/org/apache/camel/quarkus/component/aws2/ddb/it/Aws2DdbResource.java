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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

@Path("/aws2-ddb")
@ApplicationScoped
public class Aws2DdbResource {

    @ConfigProperty(name = "aws-ddb.table-name")
    String tableName;

    @Inject
    ProducerTemplate producerTemplate;

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message, @PathParam("key") String key) throws Exception {
        final Map<String, AttributeValue> item = new HashMap<String, AttributeValue>() {
            {
                put("key", AttributeValue.builder()
                        .s(key).build());
                put("value", AttributeValue.builder()
                        .s(message).build());
            }
        };
        producerTemplate.sendBodyAndHeaders(componentUri(Ddb2Operations.PutItem),
                message,
                new HashMap<String, Object>() {
                    {
                        put(Ddb2Constants.CONSISTENT_READ, true);
                        put(Ddb2Constants.ITEM, item);
                    }
                });
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @SuppressWarnings("unchecked")
    @Path("/item/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getItem(@PathParam("key") String key) throws Exception {
        final Map<String, AttributeValue> item = (Map<String, AttributeValue>) producerTemplate
                .send(componentUri(Ddb2Operations.GetItem),
                        e -> {
                            e.getMessage().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                            e.getMessage().setHeader(Ddb2Constants.ATTRIBUTE_NAMES,
                                    new HashSet<String>(Arrays.asList("key", "value")));
                            e.getMessage().setHeader(Ddb2Constants.KEY,
                                    Collections.<String, AttributeValue> singletonMap("key",
                                            AttributeValue.builder().s(key).build()));

                        })
                .getMessage()
                .getHeader(Ddb2Constants.ATTRIBUTES, Map.class);
        final AttributeValue val = item.get("value");
        return val == null ? null : val.s();
    }

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public void updateItem(String message, @PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeaders(
                componentUri(Ddb2Operations.UpdateItem),
                null,
                new HashMap<String, Object>() {
                    {
                        put(
                                Ddb2Constants.KEY,
                                Collections.singletonMap("key", AttributeValue.builder().s(key).build()));
                        put(
                                Ddb2Constants.UPDATE_VALUES,
                                Collections.singletonMap(
                                        "value",
                                        AttributeValueUpdate.builder()
                                                .action(AttributeAction.PUT)
                                                .value(AttributeValue.builder().s(message).build())
                                                .build()));
                    }
                });
    }

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteItem(@PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeaders(
                componentUri(Ddb2Operations.DeleteItem),
                null,
                new HashMap<String, Object>() {
                    {
                        put(Ddb2Constants.CONSISTENT_READ, true);
                        put(Ddb2Constants.KEY,
                                Collections.<String, AttributeValue> singletonMap("key",
                                        AttributeValue.builder().s(key).build()));
                    }
                });
    }

    private String componentUri(Ddb2Operations op) {
        return "aws2-ddb://" + tableName + "?operation=" + op;
    }

    @Path("/batchItems")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map batchItems(List<String> keyValues) throws Exception {
        Map<String, AttributeValue>[] keyAttrs = keyValues.stream()
                .map(v -> Collections.singletonMap("key", AttributeValue.builder().s(v).build())).toArray(Map[]::new);
        Map<String, KeysAndAttributes> keysAttrs = Collections.singletonMap(tableName,
                KeysAndAttributes.builder().keys(keyAttrs).build());

        Map<String, List<Map<AttributeValue, AttributeValue>>> result = (Map<String, List<Map<AttributeValue, AttributeValue>>>) producerTemplate
                .send(componentUri(Ddb2Operations.BatchGetItems),
                        e -> e.getIn().setHeader(Ddb2Constants.BATCH_ITEMS, keysAttrs))
                .getMessage().getHeader(Ddb2Constants.BATCH_RESPONSE);

        Map<String, String> collected = new HashMap<>();
        for (Map<AttributeValue, AttributeValue> m : result.get(tableName)) {
            collected.put(m.get("key").s(), m.get("value").s());
        }

        return collected;
    }

    @Path("/query")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map query(List<String> attributeNames,
            @QueryParam("startKey") String startKey) throws Exception {
        Map<String, Condition> keyConditions = new HashMap<>();
        Condition.Builder condition = Condition.builder().comparisonOperator(ComparisonOperator.GT.toString())
                .attributeValueList(AttributeValue.builder().n("1985").build());

        keyConditions.put("1", condition.build());

        List<Map<AttributeValue, AttributeValue>> result = (List<Map<AttributeValue, AttributeValue>>) producerTemplate
                .send(componentUri(Ddb2Operations.Query),
                        e -> {
                            e.getIn().setHeader(Ddb2Constants.ATTRIBUTE_NAMES, attributeNames);
                            e.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                            e.getIn().setHeader(Ddb2Constants.START_KEY, startKey);
                            e.getIn().setHeader(Ddb2Constants.LIMIT, 10);
                            e.getIn().setHeader(Ddb2Constants.SCAN_INDEX_FORWARD, true);
                            e.getIn().setHeader(Ddb2Constants.KEY_CONDITIONS, keyConditions);
                            e.getIn().setHeader(Ddb2Constants.INDEX_NAME, "key-index");
                        })
                .getMessage().getHeader(Ddb2Constants.ITEMS);

        Map<String, String> collected = new HashMap<>();
        for (Map<AttributeValue, AttributeValue> m : result) {
            collected.put(m.get("key").s(), m.get("value").s());
        }

        return collected;
    }

    @Path("/operation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map operation(String operation) throws Exception {
        final Message message = producerTemplate
                .send(componentUri(Ddb2Operations.valueOf(operation)), e -> {
                })
                .getMessage();
        return message.getHeaders().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    if (e.getValue() instanceof List) {
                        return ((List) e.getValue()).size();
                    }
                    return e.getValue().toString();
                }));
    }

}
