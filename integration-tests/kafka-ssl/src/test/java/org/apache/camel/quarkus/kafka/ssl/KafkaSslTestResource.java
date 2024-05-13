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

import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestSupport;
import org.apache.camel.util.CollectionHelper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.jboss.logging.Logger;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class KafkaSslTestResource extends KafkaTestResource {
    private static final Logger LOGGER = Logger.getLogger(KafkaSslTestResource.class);

    static final String KAFKA_KEYSTORE_PASSWORD = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L";
    static final String KAFKA_HOSTNAME = "localhost";

    private static final String KAFKA_KEYSTORE_FILE = KAFKA_HOSTNAME + "-keystore.p12";
    private static final String KAFKA_KEYSTORE_TYPE = "PKCS12";
    private static final String KAFKA_TRUSTSTORE_FILE = KAFKA_HOSTNAME + "-truststore.p12";
    private SSLKafkaContainer container;
    private GenericContainer j17container;

    @Override
    public Map<String, String> start() {

        //todo
//        KafkaTestSupport.regenerateCertificatesForDockerHost(configDir, KAFKA_CERTIFICATE_SCRIPT, KAFKA_KEYSTORE_FILE,
//                KAFKA_TRUSTSTORE_FILE);

        //if FIPS environment is present, custom container using J17 has to used because:
        // Password-based encryption support in FIPs mode was implemented in the Red Hat build of OpenJDK 17 update 4
        if (isFips()) {
            //custom image should be cached for the next usages with following id
            String customImageName = "camel-quarkus-test-custom-" + KAFKA_IMAGE_NAME.replaceAll("[\\./]", "-");

            try {
                //in case that the image is not accessible, fatch exception is thrown
                container = new SSLKafkaContainer(customImageName);
                container.waitForRunning();
                container.start();
            } catch (ContainerFetchException e) {
                if (e.getCause() instanceof NotFoundException) {
                    LOGGER.infof("Custom image for kafka (%s) does not exist. Has to be created.", customImageName);

                    j17container = new GenericContainer(
                            new ImageFromDockerfile(customImageName, false)
                                    .withDockerfileFromBuilder(builder -> builder
                                            .from("quay.io/strimzi-test-container/test-container:latest-kafka-3.2.1")
                                            .env("JAVA_HOME", "/usr/lib/jvm/jre-17")
                                            .env("PATH", "/usr/lib/jvm/jre-17/bin:$PATH")
                                            .user("root")
                                            .run("microdnf install -y --nodocs java-17-openjdk-headless glibc-langpack-en && microdnf clean all")));
                    j17container.start();

                    LOGGER.infof("Custom image for kafka (%s) has been created.", customImageName);

                    //start kafka container again
                    container = new SSLKafkaContainer(customImageName);
                    container.waitForRunning();
                    container.start();
                }
            }
        } else {
            container = new SSLKafkaContainer(KAFKA_IMAGE_NAME);
            container.waitForRunning();
            container.start();
        }

        return CollectionHelper.mapOf(
                "kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers(),
                "camel.component.kafka.brokers", container.getBootstrapServers(),
                "camel.component.kafka.security-protocol", "SSL",
                "camel.component.kafka.ssl-key-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-location",
                Paths.get("target", "certs").resolve(KAFKA_TRUSTSTORE_FILE).toString(),
                "camel.component.kafka.ssl-keystore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-type", KAFKA_KEYSTORE_TYPE,
                "camel.component.kafka.ssl-truststore-location",
                Paths.get("target", "certs").resolve(KAFKA_TRUSTSTORE_FILE).toString(),
                "camel.component.kafka.ssl-truststore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-truststore-type", KAFKA_KEYSTORE_TYPE);
    }

    @Override
    public void stop() {
        if (this.container != null) {
            try {
                this.container.stop();
            } catch (Exception e) {
                // Ignored
            }
        }
        if (this.j17container != null) {
            try {
                this.j17container.stop();
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    // KafkaContainer does not support SSL OOTB so we need some customizations
    static final class SSLKafkaContainer extends StrimziKafkaContainer {
        SSLKafkaContainer(final String dockerImageName) {
            super(dockerImageName);
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
                    Map.entry("ssl.keystore.location", "/etc/kafka/secrets/" + KAFKA_KEYSTORE_FILE),
                    Map.entry("ssl.keystore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.keystore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.truststore.location", "/etc/kafka/secrets/" + KAFKA_TRUSTSTORE_FILE),
                    Map.entry("ssl.truststore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.truststore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.endpoint.identification.algorithm", ""));

            withBrokerId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            Stream.of(KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
                    .forEach(keyStoreFile -> {
                        copyFileToContainer(MountableFile.forHostPath(Paths.get("target", "certs").resolve(keyStoreFile)),
                                "/etc/kafka/secrets/" + keyStoreFile);
                    });
        }
    }
}
