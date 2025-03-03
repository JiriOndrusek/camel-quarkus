package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AzureServiceBusTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        //a flag, that servicebus test resource should start a container
        //because in grupped tests, the servicebus container cannot be started together with azurite (port conflict)
        return Collections.singletonMap("azure.servicebus.running", "true");
    }

}
