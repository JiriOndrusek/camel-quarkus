package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuarkusLangchain4jRecorder {
    public void enforceJaxRsHttpClient() {
        if (System.getProperty("langchain4j.http.clientBuilderFactory") == null) {
            System.setProperty("langchain4j.http.clientBuilderFactory",
                    "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory");
        }
    }
}
