package org.apache.camel.quarkus.component.cics.it;

import java.nio.file.Paths;

import com.redhat.camel.component.cics.CICSComponent;
import com.redhat.camel.component.cics.CICSConfiguration;
import com.redhat.camel.component.cics.pool.CICSPooledGatewayFactory;
import com.redhat.camel.component.cics.pool.CICSSingleGatewayFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CicsProducer {

    @ConfigProperty(name = "ctg.tcp.port")
    int tcpPort;

    @ConfigProperty(name = "ctg.ssl.port")
    int sslPort;

    @ConfigProperty(name = "ctg.host")
    String ctgHost;

    @Named("cics")
    public CICSComponent Component() {
        return new CICSComponent();
    }

    @Named("cicsFactory")
    public CICSComponent factoryComponent() {
        CICSConfiguration configuration = new CICSConfiguration();

        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setPort(tcpPort);
        factory.setHost(ctgHost);
        factory.setProtocol("tcp");
        configuration.setGatewayFactory(factory);

        CICSComponent component = new CICSComponent();
        component.setConfiguration(configuration);
        return component;
    }

    @Named("cicsPooledFactory")
    public CICSComponent pooledFactoryComponent() throws Exception {
        CICSConfiguration configuration = new CICSConfiguration();

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

        configuration.setGatewayFactory(gatewayPool);

        CICSComponent component = new CICSComponent();
        component.setConfiguration(configuration);
        return component;
    }

    @Named("cicsSslFactory")
    public CICSComponent sslFactoryComponent() {
        CICSConfiguration configuration = new CICSConfiguration();

        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setPort(sslPort);
        factory.setHost(ctgHost);
        factory.setProtocol("ssl");
        factory.setSslPassword("changeit");
        factory.setSslKeyring(Paths.get("target/certs/localhost-truststore.p12").toAbsolutePath().toString());

        configuration.setGatewayFactory(factory);

        CICSComponent component = new CICSComponent();
        component.setConfiguration(configuration);
        return component;
    }

    @Named("cicsSslPooledFactory")
    public CICSComponent sslPooledFactoryComponent() throws Exception {
        CICSConfiguration configuration = new CICSConfiguration();

        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setHost(ctgHost);
        factory.setPort(sslPort);
        factory.setProtocol("ssl");
        factory.setSslPassword("changeit");
        factory.setSslKeyring(Paths.get("target/certs/localhost-truststore.p12").toAbsolutePath().toString());

        CICSPooledGatewayFactory gatewayPool = new CICSPooledGatewayFactory(factory);
        gatewayPool.setMaxTotal(50);
        gatewayPool.setMinIdle(25);
        gatewayPool.setInitialPoolSize(30);
        gatewayPool.setJmxEnabled(true);
        gatewayPool.setSslKeyring(Paths.get("target/certs/localhost-keystore.p12").toAbsolutePath().toString());
        gatewayPool.setSslPassword("changeit");
        gatewayPool.start();
        configuration.setGatewayFactory(gatewayPool);

        CICSComponent component = new CICSComponent();
        component.setConfiguration(configuration);
        return component;
    }

    @Singleton
    @Named("sslWrongFactory")
    CICSSingleGatewayFactory sslWrongFactory() throws Exception {
        CICSSingleGatewayFactory factory = new CICSSingleGatewayFactory();
        factory.setPort(sslPort);
        factory.setHost(ctgHost);
        factory.setProtocol("ssl");
        factory.setSslPassword("changeit");
        factory.setSslKeyring(Paths.get("target/certs/wrong-truststore.p12").toAbsolutePath().toString());
        return factory;
    }

}
