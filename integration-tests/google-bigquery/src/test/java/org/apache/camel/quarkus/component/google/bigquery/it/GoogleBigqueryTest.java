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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConstants;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.apache.camel.quarkus.test.support.google.GoogleProperty;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.apache.camel.util.CollectionHelper.mergeMaps;

@QuarkusTest
@QuarkusTestResource(GoogleCloudTestResource.class)
class GoogleBigqueryTest {


    @GoogleProperty(name = "google.project.id")
    String projectId;

    @GoogleProperty(name = "google.credentialsPath")
    String credentialsPath;

    @GoogleProperty(name = "google-bigquery.dataset-name")
    String dataset;

    @GoogleProperty(name = "google-bigquery.table-for-map")
    String tableNameForMap;

    @GoogleProperty(name = "google-bigquery.table-for-list")
    String tableNameForList;

    @GoogleProperty(name = "google-bigquery.table-for-template")
    String tableNameForTemplate;

    @GoogleProperty(name = "google-bigquery.table-for-partitioning")
    String tableNameForPartitioning;

    @GoogleProperty(name = "google-bigquery.table-for-insert-id")
    String tableNameForInsertId;

    @GoogleProperty(name = "google-bigquery.table-for-sql-crud")
    String tableNameForSqlCrud;

    @GoogleProperty(name = "google.bigquery.mock-url")
    String mockUrl;


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

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
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

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
        }

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

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForTemplate);
        Assertions.assertEquals(3, tr.getTotalRows());
        tr = getTableData(dataset + "." + tableNameForTemplate + "_suffix");
        Assertions.assertEquals(2, tr.getTotalRows());
    }

    //todo use header partitioning_decorator
    @Test
    //    @Disabled
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

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
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

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForInsertId);
        Assertions.assertEquals(1, tr.getTotalRows());
    }

    private TableResult getTableData(String tableId) throws Exception {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
                .newBuilder(String.format("SELECT * FROM `%s` ORDER BY id", tableId))
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

    private List<List<Object>> parseResult(TableResult tr) {
        List<List<Object>> retVal = new ArrayList<>();
        for (FieldValueList flv : tr.getValues()) {
            retVal.add(flv.stream().map(fv -> fv.getValue()).collect(Collectors.toList()));
        }
        return retVal;
    }

    @Test
    public void sqlCrudOperations() throws Exception {
        // create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId(mapOf("id", 1, "col1", 2, "col2", 3)))
                .queryParam("sql", String.format("INSERT INTO `%s.%s.%s` VALUES(@id, @col1, @col2)",
                        projectId, dataset, tableNameForSqlCrud))
                .queryParam("file", true)
                .post("/google-bigquery/executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId(mapOf("id", 2, "col1", 3, "col2", 4)))
                .queryParam("sql", String.format("INSERT INTO `%s.%s.%s` VALUES(@id, @col1, @col2)",
                        projectId, dataset, tableNameForSqlCrud))
                .post("/google-bigquery/executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId(Collections.emptyMap()))
                .queryParam("file", true)
                .queryParam("sql", String.format("SELECT * FROM `%s.%s.%s`",
                        projectId, dataset, tableNameForSqlCrud))
                .post("/google-bigquery/executeSql")
                .then()
                .statusCode(200)
                .body(is("2"));

        //update
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(CollectionHelper.mapOf("col1", 22, "id", 1))
                .queryParam("sql", String.format("UPDATE `%s.%s.%s` SET col1=@col1 WHERE id=@id",
                        projectId, dataset, tableNameForSqlCrud))
                .post("/google-bigquery/executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        TableResult tr = getTableData(dataset + "." + tableNameForSqlCrud);
        Assertions.assertEquals(2, tr.getTotalRows());
        List<List<Object>> results = parseResult(tr);
        Assertions.assertEquals("22", results.get(0).get(1));

        //delete
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .queryParam("sql", String.format("DELETE FROM `%s.%s.%s` WHERE id='1'",
                        projectId, dataset, tableNameForSqlCrud))
                .post("/google-bigquery/executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        tr = getTableData(dataset + "." + tableNameForSqlCrud);
        Assertions.assertEquals(1, tr.getTotalRows());
        results = parseResult(tr);
        Assertions.assertEquals("3", results.get(0).get(1));

    }

    private boolean isMockBackend() {
        return mockUrl != null && !"".equals(mockUrl.trim());
    }

    private Map<String, Object> mapWithJobId(Map<String, Object> map) {
        if(isMockBackend()) {
            return mergeMaps(Collections.singletonMap(GoogleBigQueryConstants.JOB_ID, "test-job-id"), map);
        }

        return map;
    }
}
