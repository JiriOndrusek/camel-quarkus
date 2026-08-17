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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code kafka} source: record key is the document id, a new record with the same key
 * replaces, a null-payload record (compacted-topic tombstone) deletes.
 */
// module-global resource: a single application instance serves every test class — restarts
// would desynchronise the persistent tracker from the volatile in-memory stores
@QuarkusTest
@QuarkusTestResource(IngestKafkaTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Langchain4jIngestKafkaTest {

    static final String TOPIC = "ingest-events";

    @Test
    @Order(1)
    void recordIsIngestedAndSameKeyReplaces() {
        send("orders/faq.txt", "Orders ship within THREE business days.");

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(searchEvents("How fast do orders ship?")
                        .stream().anyMatch(text -> text.contains("THREE")),
                        "the record must be ingested"));

        send("orders/faq.txt", "Orders ship within FIVE business days.");

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<String> hits = searchEvents("How fast do orders ship?");
                    assertTrue(hits.stream().anyMatch(text -> text.contains("FIVE")),
                            "a record with the same key must replace, got: " + hits);
                    assertFalse(hits.stream().anyMatch(text -> text.contains("THREE")),
                            "the previous version must be gone, got: " + hits);
                });
    }

    @Test
    @Order(2)
    void tombstoneRecordDeletes() {
        send("orders/faq.txt", null);

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertFalse(searchEvents("How fast do orders ship?")
                        .stream().anyMatch(text -> text.contains("business days")),
                        "a tombstone record must delete the document"));
    }

    static void send(String key, String value) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, IngestKafkaTestResource.bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            producer.send(new ProducerRecord<>(TOPIC, key, value));
            producer.flush();
        }
    }

    static List<String> searchEvents(String query) {
        return RestAssured.given()
                .queryParam("q", query)
                .queryParam("store", "events")
                .queryParam("max", 10)
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
