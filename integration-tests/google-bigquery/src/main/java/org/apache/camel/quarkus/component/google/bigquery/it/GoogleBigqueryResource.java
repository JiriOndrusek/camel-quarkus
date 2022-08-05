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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.http.HttpTransportOptions;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConnectionFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/google-bigquery")
public class GoogleBigqueryResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    MockBigQuery mockBigQuery;

    @Inject
    @ConfigProperty(name = "google.real-project.id")
    String projectId;

    @Inject
    @ConfigProperty(name = "google-bigquery.dataset-name")
    String datasetName;

    @Inject
    @ConfigProperty(name = "google.usingMockBackend")
    Boolean usingMockBackend;

    @Produces
    @Singleton
    @Named("connectionFactory")
    GoogleBigQueryConnectionFactory createConnectionFactory() {
        if (usingMockBackend) {
            return new GoogleBigQueryConnectionFactory(mockBigQuery);
        }
        return new GoogleBigQueryConnectionFactory();
    }

    @Named("bigQueryConnectionFactory")
    public GoogleBigQueryConnectionFactory bigQueryConnectionFactory() {
        Config config = ConfigProvider.getConfig();
        Optional<String> host = config.getOptionalValue("google.bigquery.host", String.class);

        if (host.isPresent()) {
            return new GoogleBigQueryConnectionFactory() {
                @Override
                public synchronized BigQuery getDefaultClient() throws Exception {
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
                            .setHost(host.get())
                            .setLocation(host.get())
                            .setProjectId(projectId)
                            .setTransportOptions(options)
                            .build()
                            .getService();
                }
            };
        }

        return null;
    }


    @Path("/insertMap")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertMap(@QueryParam("headerKey") String headerKey,
            @QueryParam("headerValue") String headerValue,
            @QueryParam("tableName") String tableName,
            Map<String, String> tableData) {
        return insert(tableName, tableData, headerKey, headerValue);
    }

    @Path("/insertList")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertList(@QueryParam("headerKey") String headerKey,
            @QueryParam("headerValue") String headerValue,
            @QueryParam("tableName") String tableName,
            List tableData) {

        return insert(tableName, tableData, headerKey, headerValue);
    }

    private Response insert(String tableName, Object tableData, String headerKey, String headerValue) {
        if (headerKey == null) {
            producerTemplate.requestBody("google-bigquery:" + projectId + ":" + datasetName + ":" + tableName
                    + "?connectionFactory=#connectionFactory", tableData);
        } else {
            producerTemplate.requestBodyAndHeaders("google-bigquery:" + projectId + ":" + datasetName + ":" + tableName,
                    tableData, Collections.singletonMap(headerKey, headerValue));
        }
        return Response.created(URI.create("https://camel.apache.org")).build();
    }
}
