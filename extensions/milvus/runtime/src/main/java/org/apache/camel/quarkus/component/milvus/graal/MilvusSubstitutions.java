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
package org.apache.camel.quarkus.component.milvus.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * GraalVM substitutions for Milvus extension to handle optional SSL providers.
 *
 * BouncyCastle and Jetty NPN/ALPN are optional SSL providers that require additional
 * dependencies not included in the Milvus extension. These classes are deleted from
 * the native image to avoid linkage errors.
 */
final class MilvusSubstitutions {

    /**
     * Delete all BouncyCastle-related SSL classes as BouncyCastle is an optional dependency.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastle")
    static final class DeleteBouncyCastle {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastleAlpnSslEngine")
    static final class DeleteBouncyCastleAlpnSslEngine {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastleAlpnSslUtils")
    static final class DeleteBouncyCastleAlpnSslUtils {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.util.BouncyCastleSelfSignedCertGenerator")
    static final class DeleteBouncyCastleSelfSignedCertGenerator {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastlePemReader")
    static final class DeleteBouncyCastlePemReader {
    }

    /**
     * Delete all Jetty NPN/ALPN-related SSL classes as Jetty NPN/ALPN are optional dependencies.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JettyNpnSslEngine")
    static final class DeleteJettyNpnSslEngine {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JettyAlpnSslEngine")
    static final class DeleteJettyAlpnSslEngine {
    }

    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
    static final class DeleteJdkNpnApplicationProtocolNegotiator {
    }
}
