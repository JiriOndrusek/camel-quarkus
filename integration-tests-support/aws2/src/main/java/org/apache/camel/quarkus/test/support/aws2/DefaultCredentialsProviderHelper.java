package org.apache.camel.quarkus.test.support.aws2;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

public class DefaultCredentialsProviderHelper {

    static final String UNABLE_TO_LOAD_CREDENTIALS_MSG = "Unable to load credentials";

    static boolean isDefaultCredentialsProviderDefinedOnSystem() {
        try {
            DefaultCredentialsProvider.create().resolveCredentials();
        } catch (Exception e) {
            //if message starts with "Unable to load credentials", allow testing
            if (e instanceof SdkClientException && e.getMessage() != null
                    && e.getMessage().startsWith(UNABLE_TO_LOAD_CREDENTIALS_MSG)) {
                return false;
            }
        }
        return true;
    }

}
