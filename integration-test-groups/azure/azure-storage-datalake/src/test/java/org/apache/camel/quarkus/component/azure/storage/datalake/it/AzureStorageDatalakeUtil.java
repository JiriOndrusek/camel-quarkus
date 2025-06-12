package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.util.Optional;

public class AzureStorageDatalakeUtil {

    public static String getRealAccountNameFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_NAME"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_NAME"));
    }

    public static String getRealAccountKeyFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_KEY"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_KEY"));
    }
}
