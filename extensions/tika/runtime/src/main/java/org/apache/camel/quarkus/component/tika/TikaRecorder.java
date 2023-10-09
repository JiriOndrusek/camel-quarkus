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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Component;
import org.apache.camel.Producer;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.camel.component.tika.TikaProducer;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

@Recorder
public class TikaRecorder {

    public RuntimeValue<TikaComponent> createTikaComponent(List<String> allowedParsers) {
        return new RuntimeValue<>(new QuarkusTikaComponent(allowedParsers));
    }

    @org.apache.camel.spi.annotations.Component("tika")
    static class QuarkusTikaComponent extends TikaComponent {
        private List<String> allowedParsers;

        public QuarkusTikaComponent(List<String> allowedParsers) {
            this.allowedParsers = allowedParsers;
        }

        @Override
        protected TikaEndpoint createEndpoint(String uri, TikaConfiguration tikaConfiguration) {

            //this condition is fullfilled only if no xml configuration or conf bean is used
            if (tikaConfiguration.getTikaConfig().getParser() instanceof DefaultParser) {
                DefaultParser dp = (DefaultParser) tikaConfiguration.getTikaConfig().getParser();
                //list of excluded parsers
                List<Class<? extends Parser>> excluded = dp.getAllComponentParsers().stream()
                        .map(p -> p.getClass())
                        .filter(p -> !allowedParsers.contains(p.getSimpleName()))
                        .collect(Collectors.toList());
                //create new parser, use original parserprovider
                DefaultParser newDP = new DefaultParser(dp.getMediaTypeRegistry(), tikaConfiguration.getTikaConfig().getServiceLoader(), excluded);
                return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, newDP);
            }
            return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, null);
        }

        static class QuarkusTikaEndpoint extends TikaEndpoint {

            private final Parser tikaParser;

            public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
                                       Parser tikaParser) {
                super(endpointUri, component, tikaConfiguration);
                this.tikaParser = tikaParser;
            }

            @Override
            public Producer createProducer() {
                return new TikaProducer(this, tikaParser);
            }

    }

}
