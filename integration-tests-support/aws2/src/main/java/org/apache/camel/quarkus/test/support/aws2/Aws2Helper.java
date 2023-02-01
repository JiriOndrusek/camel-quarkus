package org.apache.camel.quarkus.test.support.aws2;

import java.util.Locale;

import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

public class Aws2Helper {

    public static final String UNABLE_TO_LOAD_CREDENTIALS_MSG = "Unable to load credentials";

    public static boolean isDefaultCredentialsProviderDefinedOnSystem() {
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

    public static String camelServiceAcronym(LocalStackContainer.Service service) {
        switch (service) {
        case DYNAMODB:
            return "ddb";
        case DYNAMODB_STREAMS:
            return "ddbstream";
        case FIREHOSE:
            return "kinesis-firehose";
        case CLOUDWATCH:
            return "cw";
        case SECRETSMANAGER:
            return "secrets-manager";
        default:
            return service.name().toLowerCase(Locale.ROOT);
        }
    }

    public static void setAwsSysteCredentials(String accessKey, String secretKey) {
        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);

    }

    public static void clearAwsSysteCredentials() {
        //system properties has to be cleared after the test
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

}
