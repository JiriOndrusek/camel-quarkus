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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment.devui;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestDevUIService;
import org.apache.camel.quarkus.core.deployment.devui.CamelDevUIConstants;

@BuildSteps(onlyIf = IsDevelopment.class)
public class Langchain4jIngestDevUIProcessor {

    @BuildStep
    CardPageBuildItem pages() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Ingestion Pipelines")
                .componentLink("qwc-cq-ingest-pipelines.js")
                .icon("font-awesome-solid:file-import")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, CamelDevUIConstants.CONSOLE_ID_NONE));
        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem jsonRpcService() {
        return new JsonRPCProvidersBuildItem(IngestDevUIService.class);
    }

    @BuildStep
    AdditionalBeanBuildItem devUIServiceBean() {
        return AdditionalBeanBuildItem.unremovableOf(IngestDevUIService.class);
    }
}
