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
package org.apache.camel.quarkus.component.google.storage.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class RealAccountPropertyProducer implements ConfigSourceProvider {

    private final EnvPropertiesConfig envPropertiesConfig = new EnvPropertiesConfig();

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Collections.singletonList(envPropertiesConfig);
    }

    private static final class EnvPropertiesConfig implements ConfigSource {

        private final Map<String, String> values = new HashMap<String, String>() {
            {
                String location = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                String projectId = System.getenv("GOOGLE_PROJECT_ID");
                put("quarkus.google.cloud.service-account-location", location);
                put("quarkus.google.cloud.project-id", projectId);
                put(GoogleStorageHelper.CONFIG_PROPERTY_REAL_ACCOUNT, String.valueOf(location != null && projectId != null));
            }
        };

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return EnvPropertiesConfig.class.getName();
        }
    }
}
