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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.apache.tika.parser.Parser;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class TikaProcessor {

    private static final Logger LOG = Logger.getLogger(TikaProcessor.class);
    private static final String FEATURE = "camel-tika";

    private static final Set<String> NOT_NATIVE_READY_PARSERS = Set.of(
            "org.apache.tika.parser.mat.MatParser",
            "org.apache.tika.parser.journal.GrobidRESTParser",
            "org.apache.tika.parser.journal.JournalParser",
            "org.apache.tika.parser.jdbc.SQLite3Parser",
            "org.apache.tika.parser.mail.RFC822Parser",
            "org.apache.tika.parser.pkg.CompressorParser",
            "org.apache.tika.parser.geo.topic.GeoParser");

    private static final Map<String, String> PARSER_ABBREVIATIONS = Map.of(
            "pdf", "org.apache.tika.parser.pdf.PDFParser",
            "odf", "org.apache.tika.parser.odf.OpenDocumentParser");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initializeTikaParser(BeanContainerBuildItem beanContainer,
            BuildProducer<ServiceProviderBuildItem> serviceProvider)
            throws Exception {
        Map<String, List<TikaParserParameter>> parsers = getSupportedParserConfig(null,
                Optional.empty(),
                null, null);
        //        String tikaXmlConfiguration = generateTikaXmlConfiguration(parsers);
        serviceProvider.produce(new ServiceProviderBuildItem(Parser.class.getName(), parsers.keySet()));
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

    public static Map<String, List<TikaParserParameter>> getSupportedParserConfig(Optional<String> tikaConfigPath,
            Optional<String> requiredParsers,
            Map<String, Map<String, String>> parserParamMaps,
            Map<String, String> parserAbbreviations) throws Exception {
        Predicate<String> pred = p -> !NOT_NATIVE_READY_PARSERS.contains(p);
        Set<String> providerNames = getProviderNames(Parser.class.getName());
        if (tikaConfigPath.isPresent() || requiredParsers.isEmpty()) {
            return providerNames.stream().filter(pred).collect(Collectors.toMap(Function.identity(),
                    p -> Collections.<TikaParserParameter> emptyList()));
        } else {
            List<String> abbreviations = Arrays.stream(requiredParsers.get().split(",")).map(String::trim)
                    .collect(Collectors.toList());
            Map<String, String> fullNamesAndAbbreviations = abbreviations.stream()
                    .collect(Collectors.toMap(p -> getParserNameFromConfig(p, parserAbbreviations), Function.identity()));
            return providerNames.stream().filter(pred).filter(fullNamesAndAbbreviations::containsKey)
                    .collect(Collectors.toMap(Function.identity(),
                            p -> getParserConfig(p, parserParamMaps.get(fullNamesAndAbbreviations.get(p)))));
        }
    }

    private static List<TikaParserParameter> getParserConfig(String parserName, Map<String, String> parserParamMap) {
        List<TikaParserParameter> parserParams = new LinkedList<>();
        if (parserParamMap != null) {
            for (Map.Entry<String, String> entry : parserParamMap.entrySet()) {
                String paramName = camelCase(entry.getKey());
                String paramType = getParserParamType(parserName, paramName);
                parserParams.add(new TikaParserParameter(paramName, entry.getValue(), paramType));
            }
        }
        return parserParams;
    }

    private static String getParserNameFromConfig(String abbreviation, Map<String, String> parserAbbreviations) {
        if (PARSER_ABBREVIATIONS.containsKey(abbreviation)) {
            return PARSER_ABBREVIATIONS.get(abbreviation);
        }

        if (parserAbbreviations.containsKey(abbreviation)) {
            return parserAbbreviations.get(abbreviation);
        }

        throw new IllegalStateException("The custom abbreviation `" + abbreviation
                + "` can not be resolved to a parser class name, please set a "
                + "quarkus.tika.parser-name." + abbreviation + " property");
    }

    private static String getParserParamType(String parserName, String paramName) {
        try {
            Class<?> parserClass = loadParserClass(parserName);
            Method[] methods = parserClass.getMethods();
            String setterMethodName = "set" + capitalize(paramName);
            String paramType = null;
            for (Method method : methods) {
                if (method.getName().equals(setterMethodName) && method.getParameterCount() == 1) {
                    paramType = method.getParameterTypes()[0].getSimpleName().toLowerCase();
                    if (paramType.equals(boolean.class.getSimpleName())) {
                        // TikaConfig Param class does not recognize 'boolean', only 'bool'
                        // This whole reflection code is temporary anyway
                        paramType = "bool";
                    }
                    return paramType;
                }
            }
        } catch (Throwable t) {
            throw new TikaParseException(String.format("Parser %s has no %s property", parserName, paramName));
        }
        throw new TikaParseException(String.format("Parser %s has no %s property", parserName, paramName));
    }

    private static Class<?> loadParserClass(String parserName) {
        try {
            return TikaProcessor.class.getClassLoader().loadClass(parserName);
        } catch (Throwable t) {
            final String errorMessage = "Parser " + parserName + " can not be loaded";
            throw new TikaParseException(errorMessage);
        }
    }

    // Convert a property name such as "sort-by-position" to "sortByPosition"
    public static String camelCase(String paramName) {
        StringBuilder sb = new StringBuilder();
        String[] words = paramName.split("-");
        for (int i = 0; i < words.length; i++) {
            sb.append(i > 0 ? capitalize(words[i]) : words[i]);
        }
        return sb.toString();
    }

    private static String capitalize(String paramName) {
        if (paramName == null || paramName.length() == 0) {
            return paramName;
        }
        char[] chars = paramName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
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

    public static class TikaParserParameter {
        private final String name;
        private final String value;
        private final String type;

        public TikaParserParameter(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
