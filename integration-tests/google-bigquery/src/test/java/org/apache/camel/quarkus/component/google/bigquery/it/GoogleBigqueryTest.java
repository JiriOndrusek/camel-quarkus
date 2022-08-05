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
package org.apache.camel.quarkus.component.google.bigquery.it;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConstants;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.apache.camel.quarkus.test.support.google.GoogleProperty;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(GoogleCloudTestResource.class)
class GoogleBigqueryTest {

    @InjectMock
    MockBigQuery mockBigQuery;

    @Inject
    @ConfigProperty(name = "google.usingMockBackend")
    Boolean usingMockBackend;

    @GoogleProperty(name = "google.real-project.id")
    String projectId;

    @GoogleProperty(name = "google.credentialsPath")
    String credentialsPath;

    @GoogleProperty(name = "google-bigquery.dataset-name")
    String dataset;

    @GoogleProperty(name = "google-bigquery.table-name-for-map")
    String tableNameForMap;

    @GoogleProperty(name = "google-bigquery.table-name-for-list")
    String tableNameForList;

    @GoogleProperty(name = "google-bigquery.table-name-for-template")
    String tableNameForTemplate;

    @GoogleProperty(name = "google-bigquery.table-name-for-partitioning")
    String tableNameForPartitioning;

    @GoogleProperty(name = "google-bigquery.table-name-for-insert-id")
    String tableNameForInsertId;

    private Map<String, Integer> rowCounts = new HashMap<>();

    @BeforeEach
    public void setup() {
        if(usingMockBackend) {

            when(mockBigQuery.insertAll(any())).thenAnswer(invocation -> {
                InsertAllRequest request = invocation.getArgument(0, InsertAllRequest.class);
                Integer count = rowCounts.getOrDefault(request.getTable().getTable(), 0);
                count += request.getRows().size();
                rowCounts.put(request.getTable().getTable(), count);
                InsertAllResponse response = mock(InsertAllResponse.class);
                return response;
            });
        }
    }

    @Test
    public void insertMapTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 3; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForMap)
                    .post("/google-bigquery/insertMap")
                    .then()
                    .statusCode(201);
        }

        TableResult tr = getTableData(dataset + "." + tableNameForMap);
        Assertions.assertEquals(3, tr.getTotalRows());
    }

    @Test
    public void insertListTest() throws Exception {
        // Insert rows
        List data = new LinkedList();
        for (int i = 1; i <= 3; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            data.add(object);
        }

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(data)
                .queryParam("tableName", tableNameForList)
                .post("/google-bigquery/insertList")
                .then()
                .statusCode(201);

        TableResult tr = getTableData(dataset + "." + tableNameForList);
        Assertions.assertEquals(3, tr.getTotalRows());
    }

    @Test
    public void templateTableTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 5; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            if (i <= 3) {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(object)
                        .queryParam("tableName", tableNameForTemplate)
                        .post("/google-bigquery/insertMap")
                        .then()
                        .statusCode(201);
            } else {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(object)
                        .queryParam("tableName", tableNameForTemplate)
                        .queryParam("headerKey", GoogleBigQueryConstants.TABLE_SUFFIX)
                        .queryParam("headerValue", "_suffix")
                        .post("/google-bigquery/insertMap")
                        .then()
                        .statusCode(201);
            }
        }

        TableResult tr = getTableData(dataset + "." + tableNameForTemplate);
        Assertions.assertEquals(3, tr.getTotalRows());
        tr = getTableData(dataset + "." + tableNameForTemplate + "_suffix");
        Assertions.assertEquals(2, tr.getTotalRows());
    }

    @Test
    public void partitioningTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 11; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));
            object.put("part", String.valueOf(i));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForPartitioning)
                    .post("/google-bigquery/insertMap")
                    .then()
                    .statusCode(201);

        }

        TableResult tr = getTableData(dataset + "." + tableNameForPartitioning);
        Assertions.assertEquals(11, tr.getTotalRows());
    }

    @Test
    public void insertIdTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 2; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", "1");
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForInsertId)
                    .queryParam("headerKey", GoogleBigQueryConstants.INSERT_ID)
                    .queryParam("headerValue", "id")
                    .post("/google-bigquery/insertMap")
                    .then()
                    .statusCode(201);
        }

        TableResult tr = getTableData(dataset + "." + tableNameForInsertId);
        Assertions.assertEquals(1, tr.getTotalRows());
    }

    private TableResult getTableData(String tableId) throws Exception {
        if(usingMockBackend) {
            return new TableResult(null, 3, new Page<FieldValueList>() {
                @Override
                public boolean hasNextPage() {
                    return false;
                }

                @Override
                public String getNextPageToken() {
                    return null;
                }

                @Override
                public Page<FieldValueList> getNextPage() {
                    return null;
                }

                @Override
                public Iterable<FieldValueList> iterateAll() {
                    return null;
                }

                @Override
                public Iterable<FieldValueList> getValues() {
                    return null;
                }
            });
        }


        QueryJobConfiguration queryConfig = QueryJobConfiguration
                .newBuilder(String.format("SELECT * FROM `%s`", tableId))
                .setUseLegacySql(false)
                .build();

        BigQuery client = GoogleBigqueryCustomizer.getClient(projectId, credentialsPath);
        Job queryJob = client.create(JobInfo.newBuilder(queryConfig).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        return queryJob.getQueryResults();
    }
}
