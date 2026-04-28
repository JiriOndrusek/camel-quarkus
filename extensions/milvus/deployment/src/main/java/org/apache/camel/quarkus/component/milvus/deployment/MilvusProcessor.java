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
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class MilvusProcessor {

    /**
     * Bytecode transformation is disabled for milvus-sdk-java 2.6.18+ because the SDK
     * now properly shades all grpc/netty dependencies. The shaded libraries are self-contained
     * and no longer need to be remapped to Quarkus-provided io.grpc/io.netty.
     * Runtime initialization configuration (see configureRuntimeInitialization) is sufficient.
     */

    //This build step ensures that the Milvus Java SDK is indexed by Jandex.

    @BuildStep
    IndexDependencyBuildItem indexDependencie() {
        return new IndexDependencyBuildItem("io.milvus", "milvus-sdk-java");
    }

    /**
     * Configure runtime initialization for shaded netty/grpc classes in milvus-sdk-java 2.6.18+.
     * The SDK shades grpc-netty which itself shades netty, resulting in classes under
     * io.milvus.shaded.io.grpc.netty.shaded.io.netty.* that need runtime initialization.
     *
     * Only the classes that load native libraries or reference optional dependencies are marked
     * for runtime initialization. Other SSL classes can be initialized at build time.
     */
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
                "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.Conscrypt")
                // NOTE: Jetty and BouncyCastle SSL provider classes are deleted via GraalVM substitutions
                // in MilvusSubstitutions.java, so they are not listed here for runtime initialization
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitialized::produce);
    }

}
