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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.eventhubs.fluent.models.EventhubInner;
import com.azure.resourcemanager.eventhubs.models.EventHub;
import com.azure.resourcemanager.eventhubs.models.EventHubAuthorizationRule;
import org.apache.camel.quarkus.test.support.azure.AzureCloudContext;
import org.apache.camel.quarkus.test.support.azure.AzureService;
import org.apache.camel.quarkus.test.support.azure.AzureTestEnvCustomizer;

public class AzureKeyVaultTestEnvCustomizer implements AzureTestEnvCustomizer {

    @Override
    public AzureService[] services() {
        return new AzureService[] { AzureService.keyVault };
    };

    @Override
    public void customize(AzureCloudContext context) {
        final String eventhubName = "jondruse4";

        // Create an Azure Resource Manager client
        final AzureResourceManager azure = context.azureResourceManager();

        // Fetch the authorization rule
        final String resourceGroup = context.getResourceGroup();
        final String namespace = context.getNamespace();
        final String subscriptionId = context.getSubscriptionId();

        //        String namespaceId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.EventHub/namespaces/%s",
        //                subscriptionId, resourceGroup, namespace);

        // Retrieve the Event Hub Namespace by ID
        //        EventHubNamespace eventHubNamespace = azure.eventHubNamespaces().getById(namespaceId);

        //        PagedIterable<EventHubNamespace> ehNamespaces = azure.eventHubNamespaces().list();
        //        EventHubNamespace eventHubNamespace = null;
        //        for (EventHubNamespace ehNamespace : ehNamespaces) {
        //            if (namespace.equals(ehNamespace.name())) {
        //                eventHubNamespace = ehNamespace;
        //                break;
        //            }
        //        }
        //        if (eventHubNamespace == null) {
        //            throw new RuntimeException("No eventhub namespace");
        //        }

        https: //github.com/Azure/azure-sdk-for-java/blob/azure-resourcemanager-eventhubs_2.27.0-beta.1/sdk/resourcemanager/azure-resourcemanager/src/samples/java/com/azure/resourcemanager/eventhubs/generated/EventHubsCreateOrUpdateSamples.java
        azure.eventHubs()
                .manager()
                .serviceClient()
                .getEventHubs()
                .createOrUpdate(resourceGroup,
                        namespace,
                        eventhubName,
                        new EventhubInner()
                                .withPartitionCount(1L)
                                .withMessageRetentionInDays(1L));

//        //create eventhubs
//        EventHub eventHub = azure.eventHubs().define(eventhubName)
//                .withExistingNamespace(resourceGroup, namespace)
//                .withRetentionPeriodInDays(1)
//                .create();

        PagedIterable<EventHubAuthorizationRule> rules = azure.eventHubs()
                .getByName(resourceGroup, namespace, eventhubName).listAuthorizationRules();

        EventHubAuthorizationRule firstRule = null;
        for (EventHubAuthorizationRule rule : rules) {
            firstRule = rule;
            break;
        }
        if (firstRule == null) {
            throw new RuntimeException("No authorization rule found");
        }
        // Retrieve the primary connection string and save as a property
        context.property("azure-key-vault-eventhubs-connection-string", firstRule.getKeys().primaryConnectionString());

        context.closeable(() -> {
            azure.eventHubs().deleteByName(resourceGroup, namespace, eventhubName);
        });

        throw new RuntimeException("TODO emergency stop");

        //        //create eventhubs required for refresh test
        //        //force reload by sending a msg
        //        try (EventHubProducerClient client = context.client(new EventHubClientBuilder().connectionString(primaryConnectionString), EventHubClientBuilder::buildProducerClient)) {
        //            client
        //        }
    }

}
