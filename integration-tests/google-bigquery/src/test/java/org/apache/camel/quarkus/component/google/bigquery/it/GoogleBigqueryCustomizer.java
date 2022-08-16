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

import java.io.FileInputStream;
import java.util.Locale;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.RangePartitioning;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;
import org.apache.camel.quarkus.test.support.google.GoogleTestEnvCustomizer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class GoogleBigqueryCustomizer implements GoogleTestEnvCustomizer {

    private static final String TEST_PROJECT_ID = "test-project";

    private GenericContainer container;

    @Override
    public GenericContainer createContainer() {
        container = new GenericContainer("wiremock/wiremock:2.33.2")
                .withExposedPorts(8080)
                .withClasspathResourceMapping("mappings", "/home/wiremock/mappings", BindMode.READ_ONLY);
        return container;
    }

    @Override
    public void customize(GoogleCloudContext envContext) {

        try {
            // ------------------ generate names ----------------
            final String datasetName = "google_bigquery_test_dataset" +
                    (envContext.isUsingMockBackend() ? "" : "_" + randomAlphanumeric(49).toLowerCase(Locale.ROOT));
            envContext.property("google-bigquery.dataset-name", datasetName);

            final String tableNameForMap = generateName("table-for-map", envContext);
            final String tableNameForList = generateName("table-for-list", envContext);
            final String tableNameForTemplate = generateName("table-for-template", envContext);
            final String tableNameForPartitioning = generateName("table-for-partitioning", envContext);
            final String tableNameForInsertId = generateName("table-for-insert-id", envContext);
            final String tableNameForSqlCrud = generateName("table-for-sql-crud", envContext);

            if (envContext.isUsingMockBackend()) {
                if (!envContext.getProperties().containsKey("google.project.id")) {
                    envContext.property("google.project.id", TEST_PROJECT_ID);
                }
                envContext.property("google.bigquery.mock-url",
                        "http://" + container.getHost() + ":" + container.getMappedPort(8080));
                return;
            }

            final String projectId = envContext.getProperties().get("google.project.id");

            final BigQuery bigQuery = getClient(projectId, envContext.getProperties().get("google.credentialsPath"));

            // --------------- create ------------------------
            bigQuery.create(DatasetInfo.newBuilder(datasetName).build());

            final Schema schema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING));
            createTable(bigQuery, datasetName, tableNameForMap, schema, null);
            createTable(bigQuery, datasetName, tableNameForList, schema, null);
            createTable(bigQuery, datasetName, tableNameForTemplate, schema, null);
            createTable(bigQuery, datasetName, tableNameForInsertId, schema, null);

            //Numeric types can not be used as a headers parameters - see https://issues.apache.org/jira/browse/CAMEL-18382
            //new schema uses all columns of string type
            final Schema sqlSchema = Schema.of(
                    Field.of("id", StandardSQLTypeName.STRING),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING));
            createTable(bigQuery, datasetName, tableNameForSqlCrud, sqlSchema, null);

            final Schema partitioningSchema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING),
                    Field.of("part", StandardSQLTypeName.INT64));
            createTable(bigQuery, datasetName, tableNameForPartitioning, partitioningSchema, RangePartitioning.newBuilder()
                    .setField("part")
                    .setRange(
                            RangePartitioning.Range.newBuilder()
                                    .setStart(1L)
                                    .setInterval(2L)
                                    .setEnd(10L)
                                    .build())
                    .build());

            // --------------- delete ------------------------
            envContext.closeable(() -> {
                bigQuery.delete(TableId.of(datasetName, tableNameForMap));
                bigQuery.delete(TableId.of(datasetName, tableNameForList));
                bigQuery.delete(TableId.of(datasetName, tableNameForTemplate));
                bigQuery.delete(TableId.of(datasetName, tableNameForTemplate + "_suffix"));
                bigQuery.delete(TableId.of(datasetName, tableNameForPartitioning));
                bigQuery.delete(TableId.of(datasetName, tableNameForInsertId));
                bigQuery.delete(TableId.of(datasetName, tableNameForSqlCrud));

                bigQuery.delete(DatasetId.of(projectId, datasetName));
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateName(String name, GoogleCloudContext envContext) {
        String retVal = "google-bigquery-" + name +
                (envContext.isUsingMockBackend() ? "" : "-" + randomAlphanumeric(49).toLowerCase(Locale.ROOT));
        envContext.property("google-bigquery." + name, retVal);
        return retVal;
    }

    public static void createTable(BigQuery bigQuery, String datasetName, String tableName, Schema schema,
            RangePartitioning rangePartitioning) {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition;
            if (rangePartitioning == null) {
                tableDefinition = StandardTableDefinition.of(schema);
            } else {
                tableDefinition = StandardTableDefinition.newBuilder()
                        .setSchema(schema)
                        .setRangePartitioning(rangePartitioning)
                        .build();
            }
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigQuery.create(tableInfo);
        } catch (BigQueryException e) {
            throw new RuntimeException(e);
        }
    }

    static BigQuery getClient(String projectId, String credentialsPath) throws Exception {

        // Load credentials from JSON key file. If you can't set the GOOGLE_APPLICATION_CREDENTIALS
        // environment variable, you can explicitly load the credentials file to construct the
        // credentials.
        GoogleCredentials credentials;
        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
            credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
        }

        // Instantiate a client.
        return BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()
                .getService();
    }

}
