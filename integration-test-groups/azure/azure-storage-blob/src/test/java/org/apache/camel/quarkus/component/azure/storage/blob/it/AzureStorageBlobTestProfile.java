package org.apache.camel.quarkus.component.azure.storage.blob.it;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.Map;

public class AzureStorageBlobTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        //a flag, that servicebus test resource should start a container
        //because in grupped tests, the servicebus container cannot be started together with azurite (port conflict)
        return Collections.singletonMap("azure.servicebus.running", "false");
    }

}
