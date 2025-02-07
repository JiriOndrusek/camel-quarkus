package org.apache.camel.quarkus.component.azure.key.vault.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AzureKeyVaultTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        //properties have to be set via profile to not be used by different azure-* test in grouped module
        return Map.of(
                "camel.vault.azure.tenantId", System.getenv("AZURE_TENANT_ID"),
                "camel.vault.azure.clientId", System.getenv("AZURE_CLIENT_ID"),
                "camel.vault.azure.clientSecret", System.getenv("AZURE_CLIENT_SECRET"));
    }
}
