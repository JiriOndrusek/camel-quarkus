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
package org.apache.camel.quarkus.component.tika;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Component;
import org.apache.camel.Producer;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.camel.component.tika.TikaProducer;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;

@Recorder
public class TikaRecorder {

    //    public RuntimeValue<TikaComponent> createTikaComponent(List<String> allowedParsers) {
    //        return new RuntimeValue<>(new QuarkusTikaComponent(allowedParsers));
    //    }
    //
    //    @org.apache.camel.spi.annotations.Component("tika")
    //    static class QuarkusTikaComponent extends TikaComponent {
    //        private List<String> allowedParsers;
    //
    //        public QuarkusTikaComponent(List<String> allowedParsers) {
    //            this.allowedParsers = allowedParsers;
    //        }
    //
    //        @Override
    //        protected TikaEndpoint createEndpoint(String uri, TikaConfiguration tikaConfiguration) {
    //
    //            //this condition is full-filled only if no xml configuration or conf bean is used
    //            if (tikaConfiguration.getTikaConfig().getParser() instanceof DefaultParser) {
    //                DefaultParser dp = (DefaultParser) tikaConfiguration.getTikaConfig().getParser();
    //                //list of excluded parsers
    //                //                List<Class<? extends Parser>> excluded = dp.getAllComponentParsers().stream()
    //                //                        .map(p -> p.getClass())
    //                //                        //                        .filter(p -> !allowedParsers.contains(p.getSimpleName()))
    //                //                        .collect(Collectors.toList());
    //
    //                List<Class<? extends Parser>> excluded = new LinkedList<>();
    //                for (Parser parser : dp.getAllComponentParsers()) {
    //                    if (!allowedParsers.contains(parser.getClass().getCanonicalName())) {
    //                        excluded.add(parser.getClass());
    //                    }
    //                }
    //                //create a new  default parser with excluded parsers
    //                DefaultParser newDP = new DefaultParser(dp.getMediaTypeRegistry(),
    //                        tikaConfiguration.getTikaConfig().getServiceLoader(), excluded);
    //                //                return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, null);
    //                return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, new AutoDetectParser(newDP));
    //            }
    //            return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, null);
    //        }
    //
    //        static class QuarkusTikaEndpoint extends TikaEndpoint {
    //
    //            private final Parser tikaParser;
    //
    //            public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
    //                    Parser tikaParser) {
    //                super(endpointUri, component, tikaConfiguration);
    //                this.tikaParser = tikaParser;
    //            }
    //
    //            @Override
    //            public Producer createProducer() {
    //                return new TikaProducer(this, tikaParser);
    //            }
    //        }
    //
    //    }

    //    public RuntimeValue<TikaComponent> createTikaComponent(String xmlTikaConfig)
    //            throws IOException, TikaException, SAXException {
    //
    //        TikaConfig tikaConfig;
    //        try (InputStream stream = new ByteArrayInputStream(xmlTikaConfig.getBytes(StandardCharsets.UTF_8))) {
    //            tikaConfig = new TikaConfig(stream);
    //        }
    //
    //        return new RuntimeValue<>(new QuarkusTikaComponent(tikaConfig));
    //    }
    //
    //    @org.apache.camel.spi.annotations.Component("tika")
    //    static class QuarkusTikaComponent extends TikaComponent {
    //
    //        private final TikaConfig tikaConfig;
    //
    //        public QuarkusTikaComponent(TikaConfig tikaConfig) {
    //            this.tikaConfig = tikaConfig;
    //        }
    //
    //        @Override
    //        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
    //            TikaConfiguration tikaConfiguration = new TikaConfiguration();
    //
    //            TikaConfig config = resolveAndRemoveReferenceParameter(parameters, TIKA_CONFIG, TikaConfig.class);
    //            if (config != null) {
    //                tikaConfiguration.setTikaConfig(config);
    //            } else {
    //                tikaConfiguration.setTikaConfig(tikaConfig);
    //            }
    //            tikaConfiguration.setOperation(new URI(uri).getHost());
    //            TikaEndpoint endpoint = createEndpoint(uri, tikaConfiguration);
    //            setProperties(endpoint, parameters);
    //            return endpoint;
    //        }
    //    }

    //    static class QuarkusTikaEndpoint extends TikaEndpoint {
    //
    //        private final TikaParserProducer tikaParserProducer;
    //
    //        public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
    //                                   TikaParserProducer tikaParserProducer) {
    //            super(endpointUri, component, tikaConfiguration);
    //            this.tikaParserProducer = tikaParserProducer;
    //        }

    public RuntimeValue<TikaComponent> createTikaComponent(BeanContainer container, String xmlTikaConfig,
            org.apache.camel.quarkus.component.tika.TikaConfig tikaConfigAttrs)
            throws IOException, TikaException, SAXException {
        TikaConfig tikaConfig;
        try (InputStream stream = new ByteArrayInputStream(xmlTikaConfig.getBytes(StandardCharsets.UTF_8))) {
            tikaConfig = new TikaConfig(stream);
        }

        Parser parser = new AutoDetectParser(tikaConfig);
        TikaParserProducer producer = container.beanInstance(TikaParserProducer.class);
        //        producer.initialize(parser);
        producer.initialize(initializeParser(tikaConfigAttrs, xmlTikaConfig));

        return new RuntimeValue<>(new QuarkusTikaComponent(producer));
    }

    private TikaParser initializeParser(org.apache.camel.quarkus.component.tika.TikaConfig config,
            String tikaXmlConfiguration) {
        TikaConfig tikaConfig;

        try (InputStream stream = getTikaConfigStream(config, tikaXmlConfiguration)) {
            tikaConfig = new TikaConfig(stream);
        } catch (Exception ex) {
            final String errorMessage = "Invalid tika-config.xml";
            throw new TikaParseException(errorMessage, ex);
        }

        // Create a native Tika Parser. AutoDetectParser is used by default but it is wrapped
        // by RecursiveParserWrapper if the appending of the embedded content is disabled
        Parser nativeParser = new AutoDetectParser(tikaConfig);
        if (!config.appendEmbeddedContent) {
            // the recursive parser will catch the embedded exceptions by default
            nativeParser = new RecursiveParserWrapper(nativeParser, true);
        }
        return new TikaParser(nativeParser, config.appendEmbeddedContent);
    }

    private static InputStream getTikaConfigStream(org.apache.camel.quarkus.component.tika.TikaConfig config,
            String tikaXmlConfiguration) {
        // Load tika-config.xml resource
        InputStream is;
        if (config.tikaConfigPath.isPresent()) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(config.tikaConfigPath.get());
            if (is == null) {
                final String errorMessage = "tika-config.xml can not be found at " + config.tikaConfigPath.get();
                throw new TikaParseException(errorMessage);
            }
        } else {
            is = new ByteArrayInputStream(tikaXmlConfiguration.getBytes(StandardCharsets.UTF_8));
        }
        return is;
    }

    @org.apache.camel.spi.annotations.Component("tika")
    static class QuarkusTikaComponent extends TikaComponent {

        private final TikaParserProducer tikaParserProducer;

        public QuarkusTikaComponent(TikaParserProducer tikaParserProducer) {
            this.tikaParserProducer = tikaParserProducer;
        }

        @Override
        protected TikaEndpoint createEndpoint(String uri, TikaConfiguration tikaConfiguration) {
            return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, tikaParserProducer);
        }
    }

    static class QuarkusTikaEndpoint extends TikaEndpoint {

        private final TikaParserProducer tikaParserProducer;

        public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
                TikaParserProducer tikaParserProducer) {
            super(endpointUri, component, tikaConfiguration);
            this.tikaParserProducer = tikaParserProducer;
        }

        @Override
        public Producer createProducer() throws Exception {
            TikaParser tikaParser = tikaParserProducer.tikaParser();
            return new TikaProducer(this, new Parser() {
                @Override
                public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
                    return Collections.emptySet();
                }

                @Override
                public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata,
                        ParseContext parseContext) throws IOException, SAXException, TikaException {
                    //                    tikaParser.parse(inputStream, contentHandler, metadata, parseContext);
                    TikaContent tc = tikaParser.parse(inputStream, contentHandler);
                    TikaMetadata tm = tc.getMetadata();
                    if (tm != null) {
                        for (String name : tm.getNames()) {
                            tm.getValues(name).stream().forEach((v) -> metadata.add(name, v));
                        }
                    }
                }
            });
        }
    }
}
