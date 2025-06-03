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
package org.apache.camel.quarkus.component.weaviate.it;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.weaviate.WeaviateContainer;

public class WeaviateTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName WEAVIATE_IMAGE = DockerImageName
            .parse(ConfigProvider.getConfig().getValue("weaviate.container.image", String.class))
            .asCompatibleSubstituteFor("semitechnologies/weaviate");

    private WeaviateContainer container = new WeaviateContainer(WEAVIATE_IMAGE)
            .withStartupTimeout(Duration.ofMinutes(3L));

    @Override
    public Map<String, String> start() {
        container.start();

        return Map.of(
                WeaviateResource.WEAVIATE_HOST_ADDRESS, container.getHttpHostAddress());
    }

    private int getWeaviatePort() {
        URL url = null;
        try {
            url = new URL("http://" + container.getHttpHostAddress());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url.getPort();
    }

    @Override
    public void stop() {
        if (container.isRunning()) {
            container.stop();
        }
    }
    //
    //    @Override
    //    public void inject(Object testInstance) {
    //        ((MinioTest) testInstance).setEndpoint(endpoint);
    //    }
}
