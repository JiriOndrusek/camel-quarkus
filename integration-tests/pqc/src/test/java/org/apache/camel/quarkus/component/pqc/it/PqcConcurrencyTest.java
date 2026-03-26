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
package org.apache.camel.quarkus.component.pqc.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PqcConcurrencyTest {

    @Test
    public void testConcurrentSignOperations() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> RestAssured.post("/pqc/concurrent/sign")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString());
        }

        List<Future<String>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all operations completed successfully
        for (Future<String> result : results) {
            String signature = result.get();
            assertNotNull(signature);
            assertTrue(signature.length() > 0);
        }
    }

    @Test
    public void testConcurrentKemOperations() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                String response = RestAssured.post("/pqc/concurrent/kem")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
                return Boolean.parseBoolean(response);
            });
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all operations completed successfully
        for (Future<Boolean> result : results) {
            Boolean success = result.get();
            assertTrue(success);
        }
    }

    @Test
    public void testConcurrentMixedOperations() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            if (index % 2 == 0) {
                // Sign operations
                tasks.add(() -> RestAssured.post("/pqc/concurrent/sign")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString());
            } else {
                // KEM operations
                tasks.add(() -> {
                    String response = RestAssured.post("/pqc/concurrent/kem")
                            .then()
                            .statusCode(200)
                            .extract()
                            .asString();
                    return Boolean.parseBoolean(response);
                });
            }
        }

        List<Future<Object>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all operations completed successfully
        for (Future<Object> result : results) {
            Object value = result.get();
            assertNotNull(value);
        }
    }
}
