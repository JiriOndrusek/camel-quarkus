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
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * GraalVM substitutions to handle optional SSL providers and utilities that are not included
 * as dependencies in the Milvus extension.
 *
 * Some classes are deleted entirely (@Delete) while others are substituted (@Substitute) to
 * report as unavailable. This prevents GraalVM from attempting to load their dependencies
 * during the build phase, which would cause ClassNotFoundException.
 */
public class MilvusSubstitutions {

    /**
     * Delete BouncyCastlePemReader as BouncyCastle is an optional dependency not included.
     * This class uses Class.forName() to detect BouncyCastle availability, which fails
     * during GraalVM's build-time static analysis before the runtime detection can occur.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastlePemReader")
    static final class DeleteBouncyCastlePemReader {
    }

    /**
     * Delete BouncyCastle SSL provider as it's an optional dependency not included.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.BouncyCastle")
    static final class DeleteBouncyCastle {
    }

    /**
     * Delete Jetty NPN SSL engine as Jetty NPN is an optional dependency not included.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JettyNpnSslEngine")
    static final class DeleteJettyNpnSslEngine {
    }

    /**
     * Delete Jetty ALPN SSL engine as Jetty ALPN is an optional dependency not included.
     */
    @Delete
    @TargetClass(className = "io.milvus.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.JettyAlpnSslEngine")
    static final class DeleteJettyAlpnSslEngine {
    }
}
