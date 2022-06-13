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
package org.apache.camel.quarkus.test;

import java.util.Collections;
import java.util.HashMap;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class TestConfigSourceFactory implements ConfigSourceFactory {

    private final static MapBackedConfigSource source = new MapBackedConfigSource("logging_properties", new HashMap() {
        {
            put("quarkus.log.console.enable", "false");
            put("quarkus.log.file.enable", "true");
            put("quarkus.log.file.path", "target/camel-test.log");
            put("quarkus.log.file.level", "INFO");
            put("quarkus.log.file.format", "%d %-5p %c{1} - %m %n");
        }
    }) {
    };

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(source);
    }
}
