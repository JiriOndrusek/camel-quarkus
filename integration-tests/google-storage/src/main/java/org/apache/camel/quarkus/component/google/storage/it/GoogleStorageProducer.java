package org.apache.camel.quarkus.component.google.storage.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.Produces;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class GoogleStorageProducer {

    @ConfigProperty(name = GoogleStorageResource.PARAM_PORT)
    Integer port;

    @Produces
    @ApplicationScoped
    @Named("storageClient")
    public Storage produceGoodleStorage() {
        return StorageOptions.newBuilder()
                .setHost("http://localhost:" + port)
                .setProjectId("dummy-project-for-testing")
                .build()
                .getService();
    }
}
