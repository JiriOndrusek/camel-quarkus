package org.apache.camel.quarkus.test.support.kafka;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.camel.util.CollectionHelper;
import org.apache.kafka.clients.CommonClientConfigs;

public class KafkaContainerProperties {

    //    private String ssl_key_password;
    private String sslKeystoreLocation;
    //    private String ssl_keystore_password;
    private String sslKeystoreType;
    private String sslTruststoreLocation;
    //    private String ssl_truststore_password;
    private String sslTruststoreType;
    private String bootstrapServers;

    public String getSslKeystoreLocation() {
        return sslKeystoreLocation;
    }

    public KafkaContainerProperties withSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
        return this;
    }

    public String getSslKeystoreType() {
        return sslKeystoreType;
    }

    public KafkaContainerProperties withSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
        return this;
    }

    public String getSslTruststoreLocation() {
        return sslTruststoreLocation;
    }

    public KafkaContainerProperties withSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
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
                Paths.get("target", "certs").resolve(getSslKeystoreLocation()).toString(),
                prefix + ".ssl-keystore-type", getSslKeystoreType(),
                prefix + ".ssl-truststore-location",
                Paths.get("target", "certs").resolve(getSslTruststoreLocation()).toString(),
                prefix + ".ssl-truststore-type", getSslTruststoreType());
    }

}
