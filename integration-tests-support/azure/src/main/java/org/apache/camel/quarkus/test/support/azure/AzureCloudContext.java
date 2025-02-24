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
package org.apache.camel.quarkus.test.support.azure;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

import com.azure.core.client.traits.TokenCredentialTrait;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureCloudContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudContext.class);

    private final ArrayList<AutoCloseable> closeables = new ArrayList<>();
    private final Map<String, String> properties = new LinkedHashMap<>();
    private boolean usingMockBackend;

    public AzureCloudContext(AzureService[] azureServices, String accountName, String accountKey) {
        //todo
        //        if(!isUsingMockBackend()) {
        for (AzureService azureService : azureServices) {
            properties.put("azurite." + azureService + ".account.name", accountName);
            properties.put("azurite." + azureService + ".account.key", accountKey);
        }
        //        }
    }

    /**
     * Add an {@link AutoCloseable} to be closed after running Google Cloud tests
     *
     * @param  closeable the {@link AutoCloseable} to add
     * @return           this {@link AzureCloudContext}
     */
    public AzureCloudContext closeable(AutoCloseable closeable) {
        closeables.add(closeable);
        return this;
    }

    /**
     * Close all {@link AutoCloseable}s registered via {@link #closeable(AutoCloseable)}
     */
    public void close() {
        ListIterator<AutoCloseable> it = closeables.listIterator(closeables.size());
        while (it.hasPrevious()) {
            AutoCloseable c = it.previous();
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not close %s", c), e);
            }
        }
    }

    public <C, B extends TokenCredentialTrait<B>> C client(B builder, Function<B, C> buildClient) {
        if (!isUsingMockBackend()) {
            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(System.getenv("AZURE_TENANT_ID"))
                    .clientId(System.getenv("AZURE_CLIENT_ID"))
                    .clientSecret(System.getenv("AZURE_CLIENT_SECRET")).build();

            builder.credential(credential);
        }

        C client = buildClient.apply(builder);
        if (client instanceof Closeable) {
            this.closeables.add((Closeable) client);
        }
        return client;
    }

    public AzureResourceManager azureResourceManager() {
        if (!isUsingMockBackend()) {
            //            TokenCredential credential = new ClientSecretCredentialBuilder()
            //                    .httpClient(new JdkHttpClientBuilder().build())
            //                    .tenantId(System.getenv("AZURE_TENANT_ID"))
            //                    .clientId(System.getenv("AZURE_CLIENT_ID"))
            //                    .clientSecret(System.getenv("AZURE_CLIENT_SECRET")).build();
            //            DefaultAzureCredential dc = new DefaultAzureCredentialBuilder().httpClient(new JdkHttpClientBuilder().build())
            //                    .build();
            //
            //            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
            //            // Create an Azure Resource Manager client
            //            return AzureResourceManager
            //                    .configure().withHttpClient(new JdkHttpClientBuilder().build())
            //                    .authenticate(dc, profile)
            //                    .withTenantId(System.getenv("AZURE_TENANT_ID"))
            //                    //                    .withDefaultSubscription();
            //                    .withSubscription(getSubscriptionId());

            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
            TokenCredential credential = new DefaultAzureCredentialBuilder()
                    .httpClient(new JdkHttpClientBuilder().build())
                    .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                    .build();
            AzureResourceManager azure = AzureResourceManager
                    .configure().withHttpClient(new JdkHttpClientBuilder().build())
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            return azure;
        }

        return null;
    }

    public String getNamespace() {
        return System.getenv("EH_NAMESPACE");
    }

    public String getResourceGroup() {
        return System.getenv("RESOURCE_GROUP");
    }

    public String getSubscriptionId() {
        return System.getenv("SUBSCRIPTION_ID");
    }

    /**
     * Add a key-value pair to the system properties seen by google cloud tests
     *
     * @param  key
     * @param  value
     * @return       this {@link AzureCloudContext}
     */
    public AzureCloudContext property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    /**
     * @return a read-only view of {@link #properties}
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public boolean isUsingMockBackend() {
        return usingMockBackend;
    }

    void setUsingMockBackend(boolean usingMockBackend) {
        this.usingMockBackend = usingMockBackend;
    }
}
