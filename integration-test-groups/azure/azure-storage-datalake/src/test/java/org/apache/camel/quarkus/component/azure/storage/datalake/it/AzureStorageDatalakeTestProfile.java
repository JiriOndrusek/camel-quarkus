package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AzureStorageDatalakeTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        String realAzureStorageAccountName = AzureStorageDatalakeUtil.getRealAccountNameFromEnv();
        String realAzureStorageAccountKey = AzureStorageDatalakeUtil.getRealAccountKeyFromEnv();
        if (realAzureStorageAccountKey != null && realAzureStorageAccountKey != null) {
            return Map.of("azure.storage.account-name", realAzureStorageAccountName,
                    "azure.storage.account-key", realAzureStorageAccountKey);

        }

        return Collections.emptyMap();
    }
}
