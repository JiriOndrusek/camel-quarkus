package org.apache.camel.quarkus.component.azure.eventhubs.it;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.Map;

public class AzureEventhubsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        //a flag, that servicebus test resource should start a container
        //because in grupped tests, the servicebus container cannot be started together with azurite (port conflict)
        return Collections.singletonMap("azure.servicebus.running", "false");
    }

}
