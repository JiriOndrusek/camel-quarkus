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
import java.io.IOException;
import java.util.Locale;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
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
import com.google.cloud.http.HttpTransportOptions;
import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;
import org.apache.camel.quarkus.test.support.google.GoogleTestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class GoogleBigqueryCustomizer implements GoogleTestEnvCustomizer {

    private static final DockerImageName BIGQUERY_IMAGE_NAME = DockerImageName.parse("ghcr.io/goccy/bigquery-emulator:0.1.2");
    private static final int BIGQUERY_PORT = 9050;
    private GenericContainer<?> container;

    @Override
    public GenericContainer createContainer() {
        container =  new GenericContainer<>(BIGQUERY_IMAGE_NAME)
                .withClasspathResourceMapping("/data.yml", "/data.yml", BindMode.READ_ONLY)
                .withExposedPorts(BIGQUERY_PORT)
                .withCommand("/bin/bigquery-emulator", "--project", "test", "--data-from-yaml", "/data.yml")
                .waitingFor(Wait.forListeningPort());

        return container;
    }


    @Override
    public void customize(GoogleCloudContext envContext) {

        try {
            String containerUrl = String.format("http://%s:%d", container.getHost(), container.getMappedPort(BIGQUERY_PORT));
            envContext.property("google.bigquery.host", containerUrl);
            envContext.property("google.usingMockBackend", String.valueOf(envContext.isUsingMockBackend()));
            // ------------------ generate names ----------------
//            final String datasetName = "camel_quarkus_dataset_"
//                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
                        final String datasetName = "test";
            envContext.property("google-bigquery.dataset-name", datasetName);
//            final String tableNameForMap = "camel-quarkus-table-for-map-"
//                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            final String tableNameForMap = "test";
            envContext.property("google-bigquery.table-name-for-map", tableNameForMap);
            final String tableNameForList = "camel-quarkus-table-for-list-"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.table-name-for-list", tableNameForList);
            final String tableNameForTemplate = "camel-quarkus-table-for-template-"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.table-name-for-template", tableNameForTemplate);
            final String tableNameForPartitioning = "camel-quarkus-table-for-partitioning-"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.table-name-for-partitioning", tableNameForPartitioning);
            final String tableNameForInsertId = "camel-quarkus-table-for-insert-id-"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.table-name-for-insert-id", tableNameForInsertId);

            if (envContext.isUsingMockBackend()) {
                envContext.property("google.real-project.id", "test");
                return;
            }

            String projectId = envContext.getProperties().get("google.real-project.id");

            BigQuery bigQuery = getClient(projectId, envContext.getProperties().get("google.credentialsPath"), containerUrl);

            // --------------- create ------------------------
            bigQuery.create(DatasetInfo.newBuilder(datasetName).build());

            Schema schema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING));
            createTable(bigQuery, datasetName, tableNameForMap, schema, null);
            createTable(bigQuery, datasetName, tableNameForList, schema, null);
            createTable(bigQuery, datasetName, tableNameForTemplate, schema, null);
            createTable(bigQuery, datasetName, tableNameForInsertId, schema, null);

            Schema partitioningSchema = Schema.of(
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

                bigQuery.delete(DatasetId.of(projectId, datasetName));
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTable(BigQuery bigQuery, String datasetName, String tableName, Schema schema,
            RangePartitioning rangePartitioning) {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition;
            if (rangePartitioning == null) {
                tableDefinition = StandardTableDefinition.newBuilder().setSchema(schema).setType(TableDefinition.Type.TABLE).build();
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

    static BigQuery getClient(String projectId, String credentialsPath, String host) throws Exception {

        if (host != null) {
            HttpTransportOptions.Builder builder = HttpTransportOptions.newBuilder();
            HttpTransportOptions options = new HttpTransportOptions(builder) {

                @Override
                public HttpRequestInitializer getHttpRequestInitializer(ServiceOptions<?, ?> serviceOptions) {
                    return new HttpRequestInitializer() {
                        public void initialize(HttpRequest httpRequest) throws IOException {
                            httpRequest.setInterceptor(new HttpExecuteInterceptor() {
                                @Override
                                public void intercept(HttpRequest request) throws IOException {
                                    String encoding = request.getHeaders().getAcceptEncoding();
                                    if (encoding != null && encoding.equals("gzip")) {
                                        request.setEncoding(null);
                                    }
                                }
                            });
                        }
                    };
                }
            };

            return BigQueryOptions.newBuilder()
                    .setCredentials(NoCredentials.getInstance())
                    .setHost(host)
                    .setLocation(host)
                    .setProjectId(projectId)
                    .setTransportOptions(options)
                    .build()
                    .getService();
        }
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
