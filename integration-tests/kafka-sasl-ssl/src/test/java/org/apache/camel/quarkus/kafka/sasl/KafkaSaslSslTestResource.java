/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.kafka.sasl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.kafka.KafkaContainerProperties;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import static io.strimzi.test.container.StrimziZookeeperContainer.ZOOKEEPER_PORT;

public class KafkaSaslSslTestResource extends KafkaTestResource {
    static final String KAFKA_KEYSTORE_PASSWORD = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg3f";
    static final String KAFKA_HOSTNAME = KafkaTestResource.KAFKA_HOSTNAME;

    @Override
    public Map<String, String> start() {
        KafkaContainerProperties properties = start(name -> new SaslSslKafkaContainer(name, createProperties()));

        Map<String, String> map = properties.toMap("camel.component.kafka");

        String jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required "
                + "username=\"alice\" "
                + "password=\"alice-secret\";";

        map.putAll(CollectionHelper.mapOf(
                "camel.component.kafka.sasl-mechanism", "SCRAM-SHA-512",
                "camel.component.kafka.sasl-jaas-config", jaasConfig,
                "camel.component.kafka.security-protocol", "SASL_SSL",
                "camel.component.kafka.ssl-key-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-truststore-password", KAFKA_KEYSTORE_PASSWORD));

        return map;
    }

    // KafkaContainer does not support SASL SSL OOTB so we need some customizations
    static final class SaslSslKafkaContainer extends StrimziKafkaContainer {
        private KafkaContainerProperties kcp;

        SaslSslKafkaContainer(final String dockerImageName, KafkaContainerProperties kcp) {
            super(dockerImageName);
            this.kcp = kcp;
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SASL_SSL://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String protocolMap = "SASL_SSL:SASL_SSL,BROKER1:PLAINTEXT";
            Map<String, String> config = Map.ofEntries(
                    Map.entry("inter.broker.listener.name", "BROKER1"),
                    Map.entry("listener.security.protocol.map", protocolMap),
                    Map.entry("zookeeper.sasl.enabled", "false"),
                    Map.entry("sasl.enabled.mechanisms", "SCRAM-SHA-512"),
                    Map.entry("sasl.mechanism.inter.broker.protocol", "SCRAM-SHA-512"),
                    Map.entry("ssl.keystore.location", "/etc/kafka/secrets/" + kcp.getSslKeystoreFileName()),
                    Map.entry("ssl.keystore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.keystore.type", kcp.getSslKeystoreType()),
                    Map.entry("ssl.truststore.location", "/etc/kafka/secrets/" + kcp.getSslKeystoreFileName()),
                    Map.entry("ssl.truststore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.truststore.type", kcp.getSslTruststoreType()),
                    Map.entry("ssl.endpoint.identification.algorithm", ""));

            withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf");
            withBrokerId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);
            copyFileToContainer(
                    MountableFile.forClasspathResource("config/kafka_server_jaas.conf"),
                    "/etc/kafka/kafka_server_jaas.conf");

            Stream.of(kcp.getSslKeystoreFileName(), kcp.getSslTruststoreFileName())
                    .forEach(keyStoreFile -> {
                        copyFileToContainer(
                                MountableFile.forHostPath(Path.of(KafkaTestResource.CERTS_BASEDIR).resolve(keyStoreFile)),
                                "/etc/kafka/secrets/" + keyStoreFile);
                    });

            String setupUsersScript = "#!/bin/bash\n"
                    + "KAFKA_OPTS= /opt/kafka/bin/kafka-configs.sh --zookeeper localhost:" + ZOOKEEPER_PORT
                    + " --alter --add-config 'SCRAM-SHA-512=[iterations=8192,password=alice-secret]' --entity-type users --entity-name alice";

            copyFileToContainer(
                    Transferable.of(setupUsersScript.getBytes(StandardCharsets.UTF_8), 0775),
                    "/setup-users.sh");
        }

        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            try {
                execInContainer("/setup-users.sh");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
