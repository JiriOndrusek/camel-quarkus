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
package org.apache.camel.quarkus.component.splunk.it;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SplunkRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Object>> results;

    @ConfigProperty(name = SplunkResource.PARAM_REMOTE_PORT)
    Integer port;

    @Override
    public void configure() throws SQLException, IOException {
        //ConsumerType.NORMAL
        from(String.format("splunk://normal?username=admin&password=changeit&scheme=http&port=%d&delay=5000&initEarliestTime=-10s" +
                "&search=search *",
                port, SplunkResource.SOURCE_TYPE))
            .process(e -> results.get("normalResults").add(e.getMessage().getBody(Map.class)));
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<Object>> results() {
        Map<String, List<Object>> result = new HashMap<>();
        result.put("normalResults", new CopyOnWriteArrayList<>());
        return result;
    }

}
