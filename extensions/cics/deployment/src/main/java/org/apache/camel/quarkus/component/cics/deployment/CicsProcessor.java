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
package org.apache.camel.quarkus.component.cics.deployment;

import java.util.Collections;
import java.util.ListResourceBundle;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ArtifactKey;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class CicsProcessor {

    private static final Logger LOG = Logger.getLogger(CicsProcessor.class);
    private static final String FEATURE = "camel-cics";
    private static final DotName LIST_RESOURCE_BUNDLE_NAME = DotName.createSimple(ListResourceBundle.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem
                .builder(DefaultEvictionPolicy.class.getName(), "com.ibm.ctg.client.ClientResourceBundle")
                .build();
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.ibm", "ctgclient");
    }

    @BuildStep
    void resourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> imageResourceBundles,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getAllKnownSubclasses(LIST_RESOURCE_BUNDLE_NAME).stream()
                .filter(cl -> cl.name().toString().contains("CicsResourceBundle")
                        || cl.name().toString().contains("ClientResourceBundle"))
                .map(c -> new NativeImageResourceBundleBuildItem(c.name().toString()))
                .forEach(imageResourceBundles::produce);
    }

    @BuildStep
    void allClientReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getKnownClasses().stream()
                .filter(cl -> (cl.name().toString().startsWith("com.ibm.ctg")
                        || cl.name().toString().startsWith("com.ibm.cics.common"))
                        && !cl.name().toString().contains("CICSTrace"))
                .map(cl -> cl.name().toString())
                .sorted()
                .peek(System.out::println)
                .map(s -> ReflectiveClassBuildItem
                        .builder(s).methods().fields().build())
                .forEach(reflectiveClasses::produce);
    }

    //testing
    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtime) {
        runtime.produce(new RuntimeInitializedClassBuildItem("com.ibm.ctg.client.LocalCICSJavaGateway"));
        runtime.produce(new RuntimeInitializedClassBuildItem("com.ibm.ctg.client.statistics.Stat"));
        runtime.produce(new RuntimeInitializedClassBuildItem("com.ibm.ctg.util.CICSServerURL"));
        runtime.produce(new RuntimeInitializedClassBuildItem("com.ibm.ctg.client.BufferTrace"));
        //        runtime.produce(new RuntimeInitializedClassBuildItem("com.ibm.ctg.client.CICSTrace"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    RemovedResourceBuildItem removedResources() {
        return new RemovedResourceBuildItem(ArtifactKey.fromString("com.ibm:ctgclient"),
                Collections.singleton("com/ibm/ctg/client/CICSTrace.class"));
    }

    //ctg client classes
    //com.ibm.ctg.client.AuthRequest
    //com.ibm.ctg.client.AutoJavaGateway
    //com.ibm.ctg.client.BufferTrace
    //com.ibm.ctg.client.BufferTrace$1
    //com.ibm.ctg.client.BufferTrace$RedirectableHandler
    //com.ibm.ctg.client.CICSTrace
    //com.ibm.ctg.client.Callbackable
    //com.ibm.ctg.client.Channel
    //com.ibm.ctg.client.Channel$1
    //com.ibm.ctg.client.Channel$ChannelInfoImpl
    //com.ibm.ctg.client.ChannelContainer
    //com.ibm.ctg.client.ChannelContainer$ContainerInfoImpl
    //com.ibm.ctg.client.ChannelContainer$ModifiedType
    //com.ibm.ctg.client.CicsCpRequest
    //com.ibm.ctg.client.CicsResourceBundle
    //com.ibm.ctg.client.CicsResourceBundle_de
    //com.ibm.ctg.client.CicsResourceBundle_es
    //com.ibm.ctg.client.CicsResourceBundle_fr
    //com.ibm.ctg.client.CicsResourceBundle_it
    //com.ibm.ctg.client.CicsResourceBundle_ja
    //com.ibm.ctg.client.CicsResourceBundle_ko
    //com.ibm.ctg.client.CicsResourceBundle_tr
    //com.ibm.ctg.client.CicsResourceBundle_zh
    //com.ibm.ctg.client.ClientMessages
    //com.ibm.ctg.client.ClientResourceBundle
    //com.ibm.ctg.client.ClientResourceBundle_de
    //com.ibm.ctg.client.ClientResourceBundle_es
    //com.ibm.ctg.client.ClientResourceBundle_fr
    //com.ibm.ctg.client.ClientResourceBundle_it
    //com.ibm.ctg.client.ClientResourceBundle_ja
    //com.ibm.ctg.client.ClientResourceBundle_ko
    //com.ibm.ctg.client.ClientResourceBundle_tr
    //com.ibm.ctg.client.ClientResourceBundle_zh
    //com.ibm.ctg.client.ClientTraceMessages
    //com.ibm.ctg.client.ClientTraceResourceBundle
    //com.ibm.ctg.client.Container
    //com.ibm.ctg.client.Container$ContainerType
    //com.ibm.ctg.client.ECIRequest
    //com.ibm.ctg.client.ECIReturnCodes
    //com.ibm.ctg.client.EPIEndReasonCodes
    //com.ibm.ctg.client.EPIRequest
    //com.ibm.ctg.client.EPIReturnCodes
    //com.ibm.ctg.client.ESIRequest
    //com.ibm.ctg.client.ESIReturnCodes
    //com.ibm.ctg.client.FileTrace
    //com.ibm.ctg.client.FileTrace$1
    //com.ibm.ctg.client.GatewayIntercept
    //com.ibm.ctg.client.GatewayIntercept$InterceptAction
    //com.ibm.ctg.client.GatewayRequest
    //com.ibm.ctg.client.GatewayReturnCodes
    //com.ibm.ctg.client.IDID
    //com.ibm.ctg.client.IPSecurityReturnCodes
    //com.ibm.ctg.client.JSSEUtils
    //com.ibm.ctg.client.JavaGateway
    //com.ibm.ctg.client.JavaGatewayInterface
    //com.ibm.ctg.client.LocalCICSJavaGateway
    //com.ibm.ctg.client.LocalJavaGateway
    //com.ibm.ctg.client.LocalWorker
    //com.ibm.ctg.client.ResourceWrapper
    //com.ibm.ctg.client.SSLContextFactory
    //com.ibm.ctg.client.SafeIP
    //com.ibm.ctg.client.SecurityProtocols
    //com.ibm.ctg.client.SslJavaGateway
    //com.ibm.ctg.client.SslJavaGatewayHelper
    //com.ibm.ctg.client.StatsJavaGateway
    //com.ibm.ctg.client.StringPadder
    //com.ibm.ctg.client.T
    //com.ibm.ctg.client.T$FilterList
    //com.ibm.ctg.client.TAdapter
    //com.ibm.ctg.client.TFileException
    //com.ibm.ctg.client.TFileReturnCodes
    //com.ibm.ctg.client.TcpJavaGateway
    //com.ibm.ctg.client.TokenGenerator
    //com.ibm.ctg.client.TraceFormatter
    //com.ibm.ctg.client.TraceInterface
    //com.ibm.ctg.client.XARequest
    //com.ibm.ctg.client.exceptions.ChannelException
    //com.ibm.ctg.client.exceptions.ChannelNameException
    //com.ibm.ctg.client.exceptions.ContainerBidiException
    //com.ibm.ctg.client.exceptions.ContainerException
    //com.ibm.ctg.client.exceptions.ContainerExistsException
    //com.ibm.ctg.client.exceptions.ContainerNameException
    //com.ibm.ctg.client.exceptions.ContainerNotFoundException
    //com.ibm.ctg.client.exceptions.ContainerTypeException
    //com.ibm.ctg.client.exceptions.package-info
    //com.ibm.ctg.client.package-info
    //com.ibm.ctg.client.statistics.IdQueryResult
    //com.ibm.ctg.client.statistics.QueryResult
    //com.ibm.ctg.client.statistics.Stat
    //com.ibm.ctg.client.statistics.Stat$StatType
    //com.ibm.ctg.client.statistics.StatFilter
    //com.ibm.ctg.client.statistics.StatFilter$1
    //com.ibm.ctg.client.statistics.StatFilter$StatFilterType
    //com.ibm.ctg.client.statistics.StatQueryResult
    //com.ibm.ctg.client.statistics.StatResourceGroup
}
