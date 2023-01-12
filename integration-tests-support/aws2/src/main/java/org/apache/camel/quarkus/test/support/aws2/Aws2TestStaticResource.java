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
package org.apache.camel.quarkus.test.support.aws2;

import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

public final class Aws2TestStaticResource extends AbstractAws2TestResource {

    @Override
    Aws2TestEnvContext createContext(String accessKey, String secretKey, String region, boolean useDefaultCredentialsProvider,
            Optional<LocalStackContainer> localstack, Service[] exportCredentialsServices) {
        return new Aws2TestEnvContext(accessKey, secretKey, region,
                useDefaultCredentialsProvider, localstack, exportCredentialsServices, true);
    }

    @Override
    public Map<String, String> start() {
        return super.start();
    }
}
