package org.apache.camel.quarkus.component.cics.it;

import com.redhat.camel.component.cics.pool.CICSPooledGatewayFactory;
import com.redhat.camel.component.cics.pool.CICSSingleGatewayFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CicsProducer {

    @ConfigProperty(name = "tcg.tcp.port")
    int tcpPort;

    @ConfigProperty(name = "ctg.host")
    String ctgHost;

    @Singleton
    @Named("pooledFactory")
    CICSPooledGatewayFactory pooledFactory() throws Exception {
        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setHost(ctgHost);
        factory.setPort(tcpPort);
        factory.setProtocol("tcp");

        CICSPooledGatewayFactory gatewayPool = new CICSPooledGatewayFactory(factory);
        gatewayPool.setMaxTotal(50);
        gatewayPool.setMinIdle(25);
        gatewayPool.setInitialPoolSize(30);
        gatewayPool.setJmxEnabled(true);
        gatewayPool.start();

        return gatewayPool;
    }

    @Singleton
    @Named("factory")
    CICSSingleGatewayFactory factory() throws Exception {
        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setPort(tcpPort);
        factory.setHost(ctgHost);
        factory.setProtocol("tcp");
        return factory;

    }
}
