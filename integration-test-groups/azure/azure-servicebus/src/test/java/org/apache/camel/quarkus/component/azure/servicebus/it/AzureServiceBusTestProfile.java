package org.apache.camel.quarkus.component.azure.servicebus.it;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.HashMap;
import java.util.Map;

public class AzureServiceBusTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "azure-servicebus";
    }
}
