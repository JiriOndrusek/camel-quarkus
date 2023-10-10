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
package org.apache.camel.quarkus.component.tika.deployment;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.xml.sax.SAXException;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.quarkus.component.tika.TikaConfig;
import org.apache.camel.quarkus.component.tika.TikaParserProducer;
import org.apache.camel.quarkus.component.tika.TikaRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.Parser;
import org.jboss.logging.Logger;

class TikaProcessor {

    //taken from quarkus-tika
    private static final Set<String> NOT_NATIVE_READY_PARSERS = Set.of(
            "org.apache.tika.parser.mat.MatParser",
            "org.apache.tika.parser.journal.GrobidRESTParser",
            "org.apache.tika.parser.journal.JournalParser",
            "org.apache.tika.parser.jdbc.SQLite3Parser",
            "org.apache.tika.parser.mail.RFC822Parser",
            "org.apache.tika.parser.pkg.CompressorParser",
            "org.apache.tika.parser.geo.topic.GeoParser");

    private static final Logger LOG = Logger.getLogger(TikaProcessor.class);
    private static final String FEATURE = "camel-tika";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(TikaParserProducer.class);
    }

    @BuildStep
    void initializeTikaParser(BuildProducer<ReflectiveClassBuildItem> runtimeInitializedClasses,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            TikaConfig tikaConfiguration) throws Exception {

        List<String> filteredNames = getAlowedParsers(tikaConfiguration);

        serviceProvider.produce(new ServiceProviderBuildItem(Parser.class.getName(), filteredNames));

        filteredNames.stream()
                .map(c -> ReflectiveClassBuildItem.builder(c).build())
                .forEach(runtimeInitializedClasses::produce);
    }

    private List<String> getAlowedParsers(TikaConfig tikaConfiguration) throws IOException {
        Set<String> names = ServiceUtil.classNamesNamedIn(TikaProcessor.class.getClassLoader(),
                "META-INF/services/" + Parser.class.getName());

        //aply exclude/include
        List<String> filteredNames = names.stream()
                .filter(n -> !NOT_NATIVE_READY_PARSERS.contains(n))
                .filter(n -> tikaConfiguration.exclude.isEmpty() || !tikaConfiguration.exclude.get().contains(n))
                .filter(n -> tikaConfiguration.include.isEmpty() || tikaConfiguration.include.get().isEmpty()
                        || tikaConfiguration.include.get().contains(n))
                .collect(Collectors.toList());
        return filteredNames;
    }

    @BuildStep
    public void registerTikaCoreResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem tikaComponent(BeanContainerBuildItem beanContainer, TikaRecorder recorder, TikaConfig tikaConfig)
            throws IOException, TikaException, SAXException {
        return new CamelRuntimeBeanBuildItem(
                "tika",
                TikaComponent.class.getName(),
                recorder.createTikaComponent(beanContainer.getValue(), generateTikaXmlConfiguration(tikaConfig.include.get()),
                        tikaConfig));
    }

    private static String generateTikaXmlConfiguration(Set<String> parserConfig) {
        StringBuilder tikaXmlConfigurationBuilder = new StringBuilder();
        tikaXmlConfigurationBuilder.append("<properties>");
        tikaXmlConfigurationBuilder.append("<parsers>");
        for (String parser : parserConfig) {
            tikaXmlConfigurationBuilder.append("<parser class=\"").append(parser).append("\">");
            //            if (!parserEntry.getValue().isEmpty()) {
            //                appendParserParameters(tikaXmlConfigurationBuilder, parserEntry.getValue());
            //            }
            tikaXmlConfigurationBuilder.append("</parser>");
        }
        tikaXmlConfigurationBuilder.append("</parsers>");
        tikaXmlConfigurationBuilder.append("</properties>");
        return tikaXmlConfigurationBuilder.toString();
    }

}
