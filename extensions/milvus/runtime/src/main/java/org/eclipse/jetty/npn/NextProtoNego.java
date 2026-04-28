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
package org.eclipse.jetty.npn;

/**
 * Stub class to satisfy GraalVM class loading for Jetty NPN references in shaded Netty SSL classes.
 * This is not a functional implementation - it only exists to prevent ClassNotFoundException
 * during native image build when Jetty NPN is not included as a dependency.
 */
public class NextProtoNego {
    public interface Provider {
    }
}
