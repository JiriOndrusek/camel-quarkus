package org.apache.camel.quarkus.component.langchain4j.agent;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuarkusLangchain4jRecorder {
    public void enforceJaxRsHttpClient() {
        if (System.getProperty("langchain4j.http.clientBuilderFactory") == null) {
            System.setProperty("langchain4j.http.clientBuilderFactory",
                    "dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory");
        }
    }
}
