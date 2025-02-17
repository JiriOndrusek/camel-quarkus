package org.apache.camel.quarkus.test.support.azure;

import java.util.stream.Stream;

public enum AzureService {
        blob(10000),
        queue(10001),
        datalake(-1, "dfs"),
        eventhubs(blob.getAzuritePort()),
        servicebus(10002); // Datalake not supported by Azurite https://github.com/Azure/Azurite/issues/553

        private final int azuritePort;
        private final String azureServiceCode;

        AzureService(int port) {
            this(port, null);
        }

        AzureService(int port, String azureServiceCode) {
            this.azuritePort = port;
            this.azureServiceCode = azureServiceCode;
        }

        public static Integer[] getAzuritePorts() {
            return Stream.of(values())
                    .mapToInt(AzureService::getAzuritePort)
                    .filter(p -> p >= 0)
                    .boxed()
                    .toArray(Integer[]::new);
        }

        public int getAzuritePort() {
            return azuritePort;
        }

        public String getAzureServiceCode() {
            return azureServiceCode == null ? name() : azureServiceCode;
        }
}
