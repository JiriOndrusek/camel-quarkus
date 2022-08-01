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
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;
import org.apache.camel.quarkus.test.support.google.GoogleTestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.GenericContainer;

public class GoogleBigqueryCustomizer implements GoogleTestEnvCustomizer {

    @Override
    public GenericContainer createContainer() {
        throw new IllegalStateException();
    }

    @Override
    public void customize(GoogleCloudContext envContext) {
        try {
            //todo different for mock
            String projectId = envContext.getProperties().get("google.real-project.id");

            BigQuery bigQuery = getClient(projectId, envContext.getProperties().get("google.credentialsPath"));

            // ------------------ generate names ----------------
            final String datasetName = "camel_quarkus_dataset_"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.dataset-name", datasetName);
            final String tableName = "camel-quarkus-table-"
                    + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-bigquery.table-name", tableName);

            // --------------- create ------------------------
            bigQuery.create(DatasetInfo.newBuilder(datasetName).build());

            Schema schema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING));
            createTable(bigQuery, datasetName, tableName, schema);

            // --------------- delete ------------------------
            envContext.closeable(() -> {
                bigQuery.delete(TableId.of(datasetName, tableName));

                bigQuery.delete(DatasetId.of(projectId, datasetName));
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportMockBackend() {
        return false;
    }

    public static void createTable(BigQuery bigQuery, String datasetName, String tableName, Schema schema) {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
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

    @Override
    public void inject(QuarkusTestResourceLifecycleManager.TestInjector testInjector) {
    }
}
