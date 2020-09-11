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
package org.apache.camel.quarkus.component.fop.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.jboss.jandex.IndexView;

class FopProcessor {

    private static final String FEATURE = "camel-fop";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        List<String> dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.endsWith("ElementMapping"))
                .sorted()
                .peek(System.out::println)
                .collect(Collectors.toList());

        dtos.add("org.apache.fop.render.pdf.extensions.PDFExtensionHandlerFactory");
        dtos.add("org.apache.fop.render.pdf.PDFDocumentHandlerMaker");
        dtos.add("org.apache.fop.render.RendererEventProducer");

        return new ReflectiveClassBuildItem(false, false, dtos.toArray(new String[dtos.size()]));
        //        return new ReflectiveClassBuildItem(false, false, new String[] { "org.apache.fop.fo.ElementMapping",
        //                "org.apache.fop.fo.FOElementMapping", "org.apache.fop.render.pdf.extensions.PDFElementMapping",
        //                "org.apache.fop.fo.extensions.InternalElementMapping",
        //                "org.apache.fop.fo.extensions.OldExtensionElementMapping" });
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.jboss.logging", "commons-logging-jboss-logging");
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.jboss.logging", "commons-logging-jboss-logging"));
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.xmlgraphics", "fop"));
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        //use regex, once this is implemented https://github.com/quarkusio/quarkus/issues/7033
        return new NativeImageResourceBuildItem(
                "META-INF/services/org.apache.fop.fo.ElementMapping",
                "META-INF/services/org.apache.fop.render.intermediate.IFDocumentHandler",
                //                "/org/apache/fop/render/event-model.xml",
                //                "/event-model.xml",
                "org/apache/fop/render/event-model.xml",
                "com/sun/org/apache/xerces/internal/impl/msg/SAXMessages");
        //                "event-model.xml")
    }

    @BuildStep
    NativeImageResourceBuildItem initResources2() {
        //use regex, once this is implemented https://github.com/quarkusio/quarkus/issues/7033
        return new NativeImageResourceBuildItem(
                "META-INF/services/org.apache.fop.util.ContentHandlerFactory",
                "org/apache/fop/render/event-model.xml");
    }

    //    @BuildStep
    //    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImage) {
    //        nativeImage.produce(new NativeImageResourceBuildItem(
    //                "META-INF/services/org.apache.fop.events.EventExceptionManager$ExceptionFactory",
    //                "META-INF/services/org.apache.fop.render.Renderer",
    //                "META-INF/services/org.apache.fop.util.text.AdvancedMessageFormat$PartFactory",
    //                "META-INF/services/org.apache.fop.fo.ElementMapping",
    //                "META-INF/services/org.apache.fop.render.XMLHandler",
    //                "META-INF/services/org.apache.xmlgraphics.image.loader.spi.ImageConverter",
    //                "META-INF/services/org.apache.fop.fo.FOEventHandler",
    //                "META-INF/services/org.apache.fop.util.ContentHandlerFactory",
    //                "META-INF/services/org.apache.xmlgraphics.image.loader.spi.ImageLoaderFactory",
    //                "META-INF/services/org.apache.fop.render.ImageHandler",
    //                "META-INF/services/org.apache.fop.util.text.AdvancedMessageFormat$Function",
    //                "META-INF/services/org.apache.xmlgraphics.image.loader.spi.ImagePreloader",
    //                "META-INF/services/org.apache.fop.render.intermediate.IFDocumentHandler",
    //                "META-INF/services/org.apache.fop.util.text.AdvancedMessageFormat$ObjectFormatter"));

    @BuildStep
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        //org.apache.tika.parser.pdf.PDFParser (https://issues.apache.org/jira/browse/PDFBOX-4548)
        //        resource.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.pdmodel.font.PDType1Font"));
        resource.produce(new RuntimeInitializedClassBuildItem(IFUtil.class.getCanonicalName()));
        resource.produce(new RuntimeInitializedClassBuildItem(ImageImplRegistry.class.getCanonicalName()));
        //        resource.produce(new RuntimeInitializedClassBuildItem(FopFactory.class.getCanonicalName()));
        //        resource.produce(new RuntimeInitializedClassBuildItem("sun.font.TrueTypeFont"));
        //        resource.produce(new RuntimeInitializedClassBuildItem("sun.font.SunFontManager"));
        //        resource.produce(new RuntimeInitializedClassBuildItem("sun.awt.X11FontManager"));
    }

}
