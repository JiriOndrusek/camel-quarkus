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
package org.apache.camel.quarkus.component.snmp.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class SnmpProcessor {

    private static final Logger LOG = Logger.getLogger(SnmpProcessor.class);
    private static final String FEATURE = "camel-snmp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void java2wsdl(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getKnownClasses().stream()
                .filter(c -> c.name().toString().matches("org.snmp4j.smi.*Address"))
                .peek(n -> System.out.println(".." + n))
                .map(c -> c.name().toString())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);

    }

    //        IndexView index = combinedIndex.getIndex();
    //
    //        if (!cxfBuildTimeConfig.wsdlgen.java2wsdl.enabled) {
    //            log.info("Skipping " + this.getClass() + " invocation on user's request");
    //            return;
    //        }

    //        String[] services = index.getAnnotations(DotName.createSimple(WebService.class.getName()))
    //                .stream()
    //                .map(AnnotationInstance::target)
    //                .map(annotationTarget -> {
    //                    if (annotationTarget.kind().equals(AnnotationTarget.Kind.CLASS)) {
    //                        return annotationTarget.asClass();
    //                    }
    //                    return null;
    //                })
    //                .filter(ci -> ci != null)
    //                .map(classInfo -> classInfo.name().toString())
    //                .toArray(String[]::new);
    //
    //        final Path outDir = Path.of(cxfBuildTimeConfig.wsdlgen.java2wsdl.outputDir);
    //        final Map<String, String> processedClasses = new HashMap<>();
    //        boolean result = false;
    //        result |= java2wsdl(services, cxfBuildTimeConfig.wsdlgen.java2wsdl.rootParameterSet, outDir,
    //                JAVA2WSDL_CONFIG_KEY_PREFIX, processedClasses);

    //        if (!cxfBuildTimeConfig.wsdlgen.java2wsdl.enabled) {
    //            log.info("Skipping " + this.getClass() + " invocation on user's request");
    //            return;
    //        }
    //
    //        try {
    //            String[] p = new String[] { "-wsdl", "-V", "-o",
    //                    "GreeterService.wsdl",
    //                    "-d",
    //                    cxfBuildTimeConfig.wsdlgen.java2wsdl.outputDir,
    //                    "io.quarkiverse.cxf.deployment.wsdlgen.GreeterService" };
    //            new JavaToWS(p).run();
    //            nativeResources.produce(new NativeImageResourceBuildItem(
    //                    cxfBuildTimeConfig.wsdlgen.java2wsdl.outputDir + "/GreeterService.wsdl"));
    //            System.out.println("-----------------------------------");
    //        } catch (Exception e) {
    //            throw new RuntimeException(new StringBuilder("Could not run wsdl2Java").toString(),
    //                    e);
    //        }
    //
    //        nativeResources.produce(new NativeImageResourceBuildItem(
    //                cxfBuildTimeConfig.wsdlgen.java2wsdl.outputDir + "/GreeterService.wsdl"));
    //
    //        //todo remove, quickworkaround
    //        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
    //                "org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl",
    //                "org.glassfish.jaxb.runtime.v2.runtime.JaxBeanInfo").methods().build());
    //    }

    //    @BuildStep
    //    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
    //        Stream.of(
    //                UdpAddress.class,
    //                IpAddress.class,
    //                TcpAddress.class,
    //                TlsAddress.class)
    //                .map(c -> c.getCanonicalName())
    //                .map(RuntimeInitializedClassBuildItem::new)
    //                .forEach(runtimeInitializedClasses::produce);
    //    }

    @BuildStep
    NativeImageResourceBuildItem nativeResources() {
        return new NativeImageResourceBuildItem("org/snmp4j/address.properties");
    }
}
