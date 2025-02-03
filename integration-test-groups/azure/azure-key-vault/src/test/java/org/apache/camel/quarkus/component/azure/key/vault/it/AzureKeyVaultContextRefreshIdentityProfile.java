package org.apache.camel.quarkus.component.azure.key.vault.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AzureKeyVaultContextRefreshIdentityProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("camel.vault.azure.azureIdentityEnabled", "true",
                "camel.vault.azure.secrets", "cq-secret-context-refresh-identity.*",
                "camel.vault.azure.eventhubConnectionString", System.getenv("AZURE_EVENT_HUBS_FOR_IDENTITY_CONNECTION_STRING"),
                "camel.vault.azure.blobContainerName", System.getenv("AZURE_EVENT_HUBS_BLOB_CONTAINER_FOR_IDENTITY_NAME"));
    }
}
