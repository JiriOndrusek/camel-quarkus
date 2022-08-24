package org.apache.camel.quarkus.component.google.bigquery.it;

import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;

public enum GoogleBigqueryTestMode {

    realService, mockedBacked, wiremockRecording;

    public static GoogleBigqueryTestMode detectMode(GoogleCloudContext envContext) {
        boolean isUsingMockBackend = envContext.isUsingMockBackend();

        String recordEnabled = System.getProperty("wiremock.record", System.getenv("WIREMOCK_RECORD"));
        boolean isRecordingEnabled = recordEnabled != null && recordEnabled.equals("true");

        if (isRecordingEnabled) {
            if (isUsingMockBackend) {
                throw new IllegalStateException(
                        "For Wiremock recording real account has to be provided! Set GOOGLE_APPLICATION_CREDENTIALS and GOOGLE_PROJECT_ID env vars.");
            }
            return wiremockRecording;
        }

        if (isUsingMockBackend) {
            return mockedBacked;
        }

        return realService;
    }
}
