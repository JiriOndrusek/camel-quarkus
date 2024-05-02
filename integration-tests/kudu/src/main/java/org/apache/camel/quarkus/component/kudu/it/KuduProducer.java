package org.apache.camel.quarkus.component.kudu.it;


import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.apache.kudu.client.KuduClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class KuduProducer {


    @ConfigProperty(name = KuduInfrastructureTestHelper.DOCKER_HOST)
    String dockerHost;

    @ConfigProperty(name = KuduInfrastructureTestHelper.MASTER_URL)
    String masterUrl;

//    @ConfigProperty(name = KuduInfrastructureTestHelper.SERVER_URL)
//    String serverUrl;

    @Produces
    @Singleton
    @Named("kuduMasterClient")
    public KuduClient produceKuduMasterClient() {
        KuduClient.KuduClientBuilder builder = new KuduClient.KuduClientBuilder(masterUrl);
//                .requireAuthentication(true);

        return builder.build();
    }

}