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
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SplunkRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<SplunkEvent>> results;

    @ConfigProperty(name = SplunkResource.PARAM_REMOTE_PORT)
    Integer port;

    @Override
    public void configure() throws SQLException, IOException {
        //ConsumerType.REALTIME
        from(String.format(
                "splunk://realtime?username=admin&password=changeit&scheme=http&port=%d&delay=3000&initEarliestTime=rt-10s&latestTime=RAW(rt+40s)&search="
                        +
                        "search sourcetype=\"STREAM\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                port, ProducerType.STREAM.name()))
                .process(e -> {
                    //if number of received events is at least 3, there is no need to receive more -> there are a lot of events (~1MB)
                    List resultList = results.get("realtimeSearch");
                    if (resultList.size() < 3) {
                        results.get("realtimeSearch").add(e.getMessage().getBody(SplunkEvent.class));
                    }
                });

        //ConsumerType.SAVEDSEARCH
        from(String.format(
                "splunk://savedsearch?username=admin&password=changeit&scheme=http&port=%d&delay=100&initEarliestTime=-1m&savedsearch=%s",
                port, SplunkResource.SAVED_SEARCH_NAME))
                .process(e -> results.get("savedSearch").add(e.getMessage().getBody(SplunkEvent.class)))
                .routeId("savedSearchRoute").autoStartup(false);

        //ConsumerType.NORMAL
        from(String.format(
                "splunk://normal?username=admin&password=changeit&scheme=http&port=%d&delay=5000&initEarliestTime=-10s&search="
                        +
                        "search sourcetype=\"SUBMIT\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                port))
                .process(e -> results.get("normalSearch").add(e.getMessage().getBody(SplunkEvent.class)));
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<SplunkEvent>> results() {
        Map<String, List<SplunkEvent>> result = new HashMap<>();
        result.put("realtimeSearch", new CopyOnWriteArrayList<>());
        result.put("savedSearch", new CopyOnWriteArrayList<>());
        result.put("normalSearch", new CopyOnWriteArrayList<>());
        return result;
    }

}
