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
package org.apache.camel.quarkus.component.milvus.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

class MilvusProcessor {

    /**
     * Intercepts all Milvus SDK classes during the Quarkus build process to perform
     * bytecode transformation. This ensures the SDK points to the correct
     * gRPC/Netty implementations, avoiding 'Class Not Found'
     */

    @BuildStep
    void relocateAllShadedCalls(
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {

        index.getIndex().getKnownClasses().stream()
                .filter(ci -> {
                    String name = ci.name().toString();
                    return name.startsWith("io.milvus");
                })
                .forEach(ci -> {
                    transformers.produce(new BytecodeTransformerBuildItem(
                            ci.name().toString(),
                            (name, cv) -> new ShadedRelocationVisitor(cv)));
                });
    }

    //This build step ensures that the Milvus Java SDK is indexed by Jandex.

    @BuildStep
    IndexDependencyBuildItem indexDependencie() {
        return new IndexDependencyBuildItem("io.milvus", "milvus-sdk-java");
    }

    /**
     * Custom ClassRemapper used during the Quarkus build step to intercept and
     * redirect shaded Netty/gRPC calls within the Milvus SDK.
     */

    private static class ShadedRelocationVisitor extends ClassRemapper {
        public ShadedRelocationVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv, new Remapper(Opcodes.ASM9) {
                @Override
                public String map(String internalName) {
                    if (internalName == null)
                        return null;

                    if (internalName.startsWith("io/grpc/netty/shaded/io/grpc")) {
                        return internalName.replace("io/grpc/netty/shaded/io/grpc", "io/grpc");
                    }
                    if (internalName.startsWith("io/grpc/netty/shaded/io/netty")) {
                        return internalName.replace("io/grpc/netty/shaded/io/netty", "io/netty");
                    }
                    return super.map(internalName);
                }
            });
        }
    }

    @BuildStep
    void configureRuntimeInitialization(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        Stream.of(
                // Milvus client that uses shaded grpc
                "io.milvus.client.MilvusServiceClient",

                // Shaded netty tcnative - ALL classes require native libraries (tcnative-boringssl-static)
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.AsyncSSLPrivateKeyMethodAdapter",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.AsyncTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.Buffer",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateCallback",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateCallbackTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateCompressionAlgo",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateRequestedCallback",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateVerifier",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.CertificateVerifierTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.Library",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.NativeStaticallyReferencedJniMethods",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.ResultCallback",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SessionTicketKey",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SniHostNameMatcher",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSL",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLPrivateKeyMethod",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLPrivateKeyMethodDecryptTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLPrivateKeyMethodSignTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLPrivateKeyMethodTask",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLSession",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLSessionCache",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.internal.tcnative.SSLTask",

                // OpenSSL classes that load native libraries
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslEngine",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslClientContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslServerContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslClientContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslServerContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslEngine",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslSessionContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslServerSessionContext",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslAsyncPrivateKeyMethod",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.OpenSslPrivateKeyMethod",

                // Conscrypt - optional SSL provider
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.ConscryptAlpnSslEngine",
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.Conscrypt",

                // JDK NPN negotiator - deferred to runtime because its static initializer calls
                // JettyNpnSslEngine.isAvailable() which we delete via @Delete substitution
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                // NOTE: Jetty and BouncyCastle SSL provider classes are deleted via GraalVM substitutions
                // in MilvusSubstitutions.java
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitialized::produce);
    }

}
