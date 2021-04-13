package org.apache.camel.quarkus.component.google.storage.it;

public class GoogleStorageHelper {

    public static boolean isRealAccount() {
        String serviceAccountKeyFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        return serviceAccountKeyFile != null && !"".equals(serviceAccountKeyFile);
    }
}
