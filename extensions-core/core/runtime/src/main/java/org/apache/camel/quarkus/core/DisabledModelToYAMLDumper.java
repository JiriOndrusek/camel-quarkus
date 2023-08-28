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
package org.apache.camel.quarkus.core;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.spi.ModelToYAMLDumper;

public class DisabledModelToYAMLDumper implements ModelToYAMLDumper {

    @Override
    public String dumpBeansAsYaml(CamelContext context, List<Object> beans) throws Exception {
        throw new UnsupportedOperationException("Please add a dependency to camel-quarkus-yaml-io");
    }

    @Override
    public String dumpModelAsYaml(CamelContext context, NamedNode definition) throws Exception {
        throw new UnsupportedOperationException(
                "Please add a dependency to camel-quarkus-yaml-io");
    }

    @Override
    public String dumpModelAsYaml(CamelContext context, NamedNode definition, boolean resolvePlaceholders,
            boolean uriAsParameters, boolean generatedIds) throws Exception {
        throw new UnsupportedOperationException(
                "Please add a dependency to camel-quarkus-yaml-io");
    }
}
