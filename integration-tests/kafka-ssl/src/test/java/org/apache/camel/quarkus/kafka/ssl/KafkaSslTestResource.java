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
package org.apache.camel.quarkus.kafka.ssl;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.kafka.KafkaContainerProperties;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.util.CollectionHelper;
import org.jboss.logging.Logger;
import org.testcontainers.utility.MountableFile;

public class KafkaSslTestResource extends KafkaTestResource {
    private static final Logger LOGGER = Logger.getLogger(KafkaSslTestResource.class);

    static final String KAFKA_KEYSTORE_PASSWORD = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L";

    @Override
    public Map<String, String> start() {
        KafkaContainerProperties properties = start(name -> new SSLKafkaContainer(name, createProperties()));

        Map<String, String> map = properties.toMap("camel.component.kafka");
        map.putAll(CollectionHelper.mapOf(
                "camel.component.kafka.security-protocol", "SSL",
                "camel.component.kafka.ssl-key-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-truststore-password", KAFKA_KEYSTORE_PASSWORD));

        return map;
    }

    // KafkaContainer does not support SSL OOTB so we need some customizations
    static final class SSLKafkaContainer extends StrimziKafkaContainer {
        private KafkaContainerProperties kcp;

        SSLKafkaContainer(final String dockerImageName, KafkaContainerProperties kcp) {
            super(dockerImageName);

            this.kcp = kcp;
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SSL://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String protocolMap = "SSL:SSL,BROKER1:PLAINTEXT";
            Map<String, String> config = Map.ofEntries(
                    Map.entry("inter.broker.listener.name", "BROKER1"),
                    Map.entry("listener.security.protocol.map", protocolMap),
                    Map.entry("ssl.keystore.location", "/etc/kafka/secrets/" + kcp.getSslKeystoreFileName()),
                    Map.entry("ssl.keystore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.keystore.type", kcp.getSslKeystoreType()),
                    Map.entry("ssl.truststore.location", "/etc/kafka/secrets/" + kcp.getSslKeystoreFileName()),
                    Map.entry("ssl.truststore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.truststore.type", kcp.getSslTruststoreType()),
                    Map.entry("ssl.endpoint.identification.algorithm", ""));

            withBrokerId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            Stream.of(kcp.getSslKeystoreFileName(), kcp.getSslTruststoreFileName())
                    .forEach(keyStoreFile -> {
                        copyFileToContainer(
                                MountableFile.forHostPath(Path.of(KafkaTestResource.CERTS_BASEDIR).resolve(keyStoreFile)),
                                "/etc/kafka/secrets/" + keyStoreFile);
                    });
        }
    }
}
