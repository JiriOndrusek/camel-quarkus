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

import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.apache.tika.parser.Parser;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class TikaProcessor {

    private static final Logger LOG = Logger.getLogger(TikaProcessor.class);
    private static final String FEATURE = "camel-tika";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void reflectiveParsers(BuildProducer<ReflectiveClassBuildItem> runtimeInitializedClasses,
            CombinedIndexBuildItem combinedIndex) throws Exception {
        IndexView index = combinedIndex.getIndex();

        Set<String> names = getProviderNames(Parser.class.getName());
        System.out.println("*********************************************");
        System.out.println(names);
        System.out.println("*********************************************");

        index.getKnownClasses().stream()
                .filter(c -> c.name().toString().endsWith("Parser"))
                .peek(n -> System.out.println(".." + n)) //todo remove
                .map(c -> c.name().toString())
                .map(c -> ReflectiveClassBuildItem.builder(c).build())
                .forEach(runtimeInitializedClasses::produce);

    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.tika", "tika-parser-microsoft-module");
    }

    private static Set<String> getProviderNames(String serviceProviderName) throws Exception {
        return ServiceUtil.classNamesNamedIn(TikaProcessor.class.getClassLoader(),
                "META-INF/services/" + serviceProviderName);
    }

    //    /*
    //     * The tika component is programmatically configured by the extension thus
    //     * we can safely prevent camel to instantiate a default instance.
    //     */
    //    @BuildStep
    //    CamelServiceFilterBuildItem serviceFilter() {
    //        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("tika"));
    //    }

    //    @Record(ExecutionTime.STATIC_INIT)
    //    @BuildStep
    //    CamelRuntimeBeanBuildItem tikaComponent(BeanContainerBuildItem beanContainer, TikaRecorder recorder) {
    //        return new CamelRuntimeBeanBuildItem(
    //                "tika",
    //                TikaComponent.class.getName(),
    //                recorder.createTikaComponent(beanContainer.getValue()));
    //    }

    //    @BuildStep
    //    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
    //        return new RuntimeInitializedClassBuildItem("org.apache.pdfbox.text.LegacyPDFStreamEngine");
    //    }

    //    @BuildStep
    //    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        //org.apache.tika.parser.pdf.PDFParser (https://issues.apache.org/jira/browse/PDFBOX-4548)
    //        resource.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.pdmodel.font.PDType1Font"));
    //        resource.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.text.LegacyPDFStreamEngine"));
    //    }
    //
    @BuildStep
    public void registerTikaCoreResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        //        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }
    //
    //    @BuildStep
    //    public void registerTikaParsersResources(BuildProducer<NativeImageResourceBuildItem> resource) {
    //        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/pdf/PDFParser.properties"));
    //    }
    //
    //    @BuildStep
    //    public void registerPdfBoxResources(BuildProducer<NativeImageResourceDirectoryBuildItem> resource) {
    //        resource.produce(new NativeImageResourceDirectoryBuildItem("org/apache/pdfbox/resources/afm"));
    //        resource.produce(new NativeImageResourceDirectoryBuildItem("org/apache/pdfbox/resources/glyphlist"));
    //        resource.produce(new NativeImageResourceDirectoryBuildItem("org/apache/fontbox/cmap"));
    //        resource.produce(new NativeImageResourceDirectoryBuildItem("org/apache/fontbox/unicode"));
    //    }

}
