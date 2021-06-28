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
package org.apache.camel.quarkus.component.avro.rpc.deployment;

import java.nio.file.Paths;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import org.apache.avro.specific.AvroGenerated;
import org.apache.camel.component.avro.AvroComponent;
import org.apache.camel.quarkus.component.avro.rpc.AvroRpcConfig;
import org.apache.camel.quarkus.component.avro.rpc.AvroRpcRecorder;
import org.apache.camel.quarkus.component.avro.rpc.AvroRpcServlet;
import org.apache.camel.quarkus.component.avro.rpc.spi.FakeHttpServer;
import org.apache.camel.quarkus.component.avro.rpc.spi.UndertowHttpServerFactory;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class AvroRpcProcessor {

    private static final String FEATURE = "camel-avro-rpc";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        IndexView index = combinedIndex.getIndex();
        String[] dtos = index.getAnnotations(DotName.createSimple(AvroGenerated.class.getName())).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(a -> a.target().asClass().name().toString())
                .toArray(String[]::new);

        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(false, false, dtos));
        reflectiveClassProducer
                .produce(new ReflectiveClassBuildItem(false, false, AvroRpcServlet.class,
                        UndertowHttpServerFactory.class, FakeHttpServer.class));
        reflectiveClassProducer
                .produce(new ReflectiveClassBuildItem(false, false, "io.undertow.vertx.VertxUndertowEngine"));
    }

    @BuildStep
    void registerDependencyForIndex(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.avro", "avro-ipc"));
    }

    @BuildStep
    void nativeImageResourceBuildItem(BuildProducer<NativeImageResourceBuildItem> resourcesProducer) {
        resourcesProducer.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.undertow.httpcore.UndertowEngine"));
    }

    @BuildStep
    CamelServiceBuildItem httpFactory() {
        return new CamelServiceBuildItem(Paths.get("META-INF/services/org/apache/camel/avro-rpc-http-server-factory"),
                UndertowHttpServerFactory.class.getName());
    }

    @BuildStep
    void servlet(BuildProducer<ServletBuildItem> servletProducer, AvroRpcConfig avroRpcConfig) {

        if (avroRpcConfig.httpReflectMapping.isPresent() && avroRpcConfig.httpSpecificMapping.isPresent() &&
                avroRpcConfig.httpReflectMapping.get().equals(avroRpcConfig.httpSpecificMapping.get())) {
            throw new IllegalStateException("Specific and reflect mapping can not use the same path!");
        }

        if (!avroRpcConfig.httpReflectMapping.isPresent() && !avroRpcConfig.httpSpecificMapping.isPresent()) {
            servletProducer.produce(
                    ServletBuildItem.builder(FEATURE, AvroRpcServlet.class.getName())
                            .addMapping("/*")
                            .build());
        } else {
            if (avroRpcConfig.httpSpecificMapping.isPresent()) {
                servletProducer.produce(
                        ServletBuildItem.builder(FEATURE + "Specific", AvroRpcServlet.class.getName())
                                .addMapping(avroRpcConfig.httpSpecificMapping.get())
                                .addInitParam("avro-rpc-specific", "true")
                                .build());
            }
            if (avroRpcConfig.httpReflectMapping.isPresent()) {
                servletProducer.produce(
                        ServletBuildItem.builder(FEATURE + "Reflect", AvroRpcServlet.class.getName())
                                .addMapping(avroRpcConfig.httpReflectMapping.get())
                                .addInitParam("avro-rpc-reflect", "true")
                                .build());
            }
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem configureBraintreeComponent(AvroRpcRecorder recorder) {
        return new CamelBeanBuildItem("avro", AvroComponent.class.getName(),
                recorder.configureAvroComponent());
    }

}
