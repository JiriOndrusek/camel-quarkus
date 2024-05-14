package org.apache.camel.quarkus.test.support.kafka;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.camel.util.CollectionHelper;
import org.apache.kafka.clients.CommonClientConfigs;

public class KafkaContainerProperties {

    private String sslKeystoreFileName;
    private String sslKeystoreType;
    private String sslTruststoreFileName;
    private String sslTruststoreType;
    private String bootstrapServers;

    public String getSslKeystoreFileName() {
        return sslKeystoreFileName;
    }

    public KafkaContainerProperties withSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreFileName = sslKeystoreLocation;
        return this;
    }

    public String getSslKeystoreType() {
        return sslKeystoreType;
    }

    public KafkaContainerProperties withSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
        return this;
    }

    public String getSslTruststoreFileName() {
        return sslTruststoreFileName;
    }

    public KafkaContainerProperties withSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreFileName = sslTruststoreLocation;
        return this;
    }

    public String getSslTruststoreType() {
        return sslTruststoreType;
    }

    public KafkaContainerProperties withSslTruststoreType(String sslTruststoreType) {
        this.sslTruststoreType = sslTruststoreType;
        return this;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public KafkaContainerProperties withBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    public Map<String, String> toMap(String prefix) {
        return CollectionHelper.mapOf(
                "kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(),
                prefix + ".brokers", getBootstrapServers(),
                prefix + ".ssl-keystore-location",
                Paths.get("target", "certs").resolve(getSslKeystoreFileName()).toString(),
                prefix + ".ssl-keystore-type", getSslKeystoreType(),
                prefix + ".ssl-truststore-location",
                Paths.get("target", "certs").resolve(getSslTruststoreFileName()).toString(),
                prefix + ".ssl-truststore-type", getSslTruststoreType());
    }

}
