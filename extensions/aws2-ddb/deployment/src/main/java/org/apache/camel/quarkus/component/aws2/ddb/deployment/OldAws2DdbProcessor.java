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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.component.aws2.ddb.CamelAmazonClientApacheTransportRecorder;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

class OldAws2DdbProcessor {
    private static final String FEATURE = "camel-aws2-ddb";

    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    private static final List<String> INTERCEPTOR_PATHS = Arrays.asList(
            "software/amazon/awssdk/global/handlers/execution.interceptors",
            "software/amazon/awssdk/services/dynamodb/execution.interceptors");

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());

    //    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    //    @BuildStep
    void process(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        INTERCEPTOR_PATHS.forEach(path -> resource.produce(new NativeImageResourceBuildItem(path)));

        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()])));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false,
                String.class.getCanonicalName()));
    }

    //    @BuildStep
    void archiveMarkers(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> archiveMarkers) {
        archiveMarkers.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_APPLICATION_ARCHIVE_MARKERS));
    }

    //    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // This class triggers initialization of FullJitterBackoffStragegy so needs to get runtime-initialized
        // as well
        producer.produce(
                new RuntimeInitializedClassBuildItem("software.amazon.awssdk.services.dynamodb.DynamoDbRetryPolicy"));
    }

    //    void crerateClient(BuildProducer<AmazonClientBuildItem> clientBuilder) {
    //        clientBuilder.produce();
    //    }

    //    AutoInjectAnnotationBuildItem test() {
    //        return AutoInjectAnnotationBuildItem.beanClassNames(DynamoDbClient.class.getName());
    //    }
    //
    //    ConfigPropertyBuildItem test() {
    //        return new ConfigPropertyBuildItem("quarkus.dynamodb.sync-client.type", "apache")
    //    }

    //    @BuildStep
    protected void setupExtension(BuildProducer<AmazonClientBuildItem> clientProducer) {

        Optional<DotName> syncClassName = Optional
                .of(DotName.createSimple("software.amazon.awssdk.services.dynamodb.DynamoDbClient"));
        Optional<DotName> asyncClassName = Optional.empty();

        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            SyncHttpClientBuildTimeConfig buildTimeSyncConfig = new SyncHttpClientBuildTimeConfig();
            buildTimeSyncConfig.type = SyncHttpClientBuildTimeConfig.SyncClientType.APACHE;

            SdkBuildTimeConfig buildTimeSdkConfig = new SdkBuildTimeConfig();
            buildTimeSdkConfig.interceptors = Optional.empty();

            clientProducer.produce(new AmazonClientBuildItem(syncClassName, Optional.empty(), "camelDynamoDb",
                    buildTimeSdkConfig, buildTimeSyncConfig));
        }
    }

    //    @BuildStep
    //    @Record(ExecutionTime.RUNTIME_INIT)
    protected void createApacheSyncTransportBuilder(List<AmazonClientBuildItem> amazonClients,
            CamelAmazonClientApacheTransportRecorder recorder,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {
        Optional<AmazonClientBuildItem> matchingClientBuildItem = amazonClients.stream().filter((c) -> {
            return c.getAwsClientName().equals("camelDynamoDb");
        }).findAny();
        matchingClientBuildItem.ifPresent((client) -> {
            if (client.getSyncClassName().isPresent()) {
                //                if (buildSyncConfig.type == SyncHttpClientBuildTimeConfig.SyncClientType.APACHE) {
                clientSyncTransports.produce(new AmazonClientSyncTransportBuildItem(client.getAwsClientName(),
                        (DotName) client.getSyncClassName().get(), recorder.configureSync("camelDynamoDb", syncConfig)));
                //                }
            }
        });
    }

}
