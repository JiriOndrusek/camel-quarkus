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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;

@ApplicationScoped
public class SplunkRouteBuilder extends RouteBuilder {

    @Inject
    CamelContext camelContext;

    public void configure() throws Exception {
        SplunkComponent sc = camelContext.getComponent("splunk", SplunkComponent.class);
        sc.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());

        camelContext.getRegistry().bind("search", String.format(
                "search index=%s sourcetype=%s",
                "submitindex", SplunkResource.SOURCE_TYPE));
        //        String url = String.format(
        //                "splunk://realtime?&scheme=http&port=32925&delay=5000&initEarliestTime=-10s&latestTime=now&search=${search}");

        String port = System.getProperty(SplunkResource.PARAM_REMOTE_PORT);
        //
        String url = "splunk://realtime?delay=5000&initEarliestTime=rt-1m&port=" + port
                + "&scheme=http&search=search+index%3Dsubmitindex+sourcetype%3DtestSource+%7C+rex+field%3D_raw+%22Name%3A+%28%3F%3Cname%3E.*%29+From%3A+%28%3F%3Cfrom%3E.*%29%22";

        //        from(url)
        //                .log(">>>>>>>>>>>>>>>>>>>>>> ${body}")
        //                .to("direct:out");
    }
}
