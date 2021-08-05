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
package org.apache.camel.quarkus.component.aws2.ddb.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.dynamodb.runtime.DynamodbBuildTimeConfig;
import io.quarkus.amazon.dynamodb.runtime.DynamodbClientProducer;
import io.quarkus.amazon.dynamodb.runtime.DynamodbConfig;
import io.quarkus.amazon.dynamodb.runtime.DynamodbRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

public class Aws2DdbProcessor extends AbstractAmazonServiceProcessor {

    private static final String FEATURE = "camel-aws2-ddb";

    DynamodbBuildTimeConfig buildTimeConfig;

    public Aws2DdbProcessor() {
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    //TODO how to avoid this?
    protected Feature amazonServiceClientName() {
        return Feature.WEBSOCKETS;
    }

    protected String configName() {
        return FEATURE;
    }

    protected DotName syncClientName() {
        return DotName.createSimple(DynamoDbClient.class.getName());
    }

    protected DotName asyncClientName() {
        return DotName.createSimple(DynamoDbAsyncClient.class.getName());
    }

    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/dynamodb/execution.interceptors";
    }

    @BuildStep
    protected void setupExtension(BuildProducer<AmazonClientBuildItem> clientProducer) {

        Optional<DotName> syncClassName = Optional
                .of(DotName.createSimple("software.amazon.awssdk.services.dynamodb.DynamoDbClient"));
        Optional<DotName> asyncClassName = Optional.empty();

        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            SyncHttpClientBuildTimeConfig buildTimeSyncConfig = new SyncHttpClientBuildTimeConfig();
            buildTimeSyncConfig.type = SyncHttpClientBuildTimeConfig.SyncClientType.APACHE;

            SdkBuildTimeConfig buildTimeSdkConfig = new SdkBuildTimeConfig();
            buildTimeSdkConfig.interceptors = Optional.empty();

            clientProducer.produce(new AmazonClientBuildItem(syncClassName, Optional.empty(), FEATURE,
                    buildTimeSdkConfig, buildTimeSyncConfig));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(DynamodbClientProducer.class);
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        producer.produce(new RuntimeInitializedClassBuildItem("software.amazon.awssdk.services.dynamodb.DynamoDbRetryPolicy"));
    }

    @BuildStep
    void setup(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<AmazonClientBuildItem> clientProducer) {
        this.setupExtension(beanRegistrationPhase, extensionSslNativeSupport, feature, interceptors, clientProducer,
                this.buildTimeConfig.sdk, this.buildTimeConfig.syncClient);
    }

    @BuildStep(onlyIf = { AmazonHttpClients.IsAmazonApacheHttpServicePresent.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder, DynamodbConfig runtimeConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {
        SyncHttpClientBuildTimeConfig buildTimeSyncConfig = new SyncHttpClientBuildTimeConfig();
        buildTimeSyncConfig.type = SyncHttpClientBuildTimeConfig.SyncClientType.APACHE;

        this.createApacheSyncTransportBuilder(amazonClients, transportRecorder, buildTimeSyncConfig,
                recorder.getSyncConfig(runtimeConfig), syncTransports);
    }

    @BuildStep(onlyIf = { AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder, DynamodbConfig runtimeConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {
        this.createUrlConnectionSyncTransportBuilder(amazonClients, transportRecorder, this.buildTimeConfig.syncClient,
                recorder.getSyncConfig(runtimeConfig), syncTransports);
    }

    @BuildStep(onlyIf = { AmazonHttpClients.IsAmazonNettyHttpServicePresent.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder, DynamodbConfig runtimeConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {
        this.createNettyAsyncTransportBuilder(amazonClients, transportRecorder, recorder.getAsyncConfig(runtimeConfig),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(DynamodbRecorder recorder, AmazonClientRecorder commonRecorder, DynamodbConfig runtimeConfig,
            List<AmazonClientSyncTransportBuildItem> syncTransports, List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        this.createClientBuilders(commonRecorder, recorder.getAwsConfig(runtimeConfig), recorder.getSdkConfig(runtimeConfig),
                this.buildTimeConfig.sdk, syncTransports, asyncTransports, DynamoDbClientBuilder.class, (syncTransport) -> {
                    return recorder.createSyncBuilder(runtimeConfig, syncTransport);
                }, DynamoDbAsyncClientBuilder.class, (asyncTransport) -> {
                    return recorder.createAsyncBuilder(runtimeConfig, asyncTransport);
                }, syntheticBeans);
    }
}
