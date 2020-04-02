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
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tika.TikaContent;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParser;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;

@Recorder
public class TikaRecorder {

    /**
     *
     * todo remove??
     */
    public RuntimeValue<TikaComponent> createTikaComponent() {
        return new RuntimeValue<>(new QuarkusTikaComponent());
    }

    static class QuarkusTikaComponent extends TikaComponent {
        private static final String TIKA_CONFIG = "tikaConfig";

        @Override
        protected TikaEndpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)  throws Exception {
            TikaConfiguration tikaConfiguration = new TikaConfiguration();
            setProperties(tikaConfiguration, parameters);
            TikaConfig config = resolveAndRemoveReferenceParameter(parameters, TIKA_CONFIG, TikaConfig.class);
            if (config == null) {
                TikaParser tikaParser = getCamelContext().getRegistry().findByType(TikaParser.class).iterator().next();
                config = new TikaConfig() {
                    @Override
                    public Parser getParser() {
                        return new  Parser() {
                            @Override
                            public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
                                return Arrays.stream(OfficeParser.POIFSDocumentType.values()).map(t -> t.getType()).collect(Collectors.toSet());
                            }

                            @Override
                            public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata,
                                    ParseContext parseContext) throws IOException, SAXException, TikaException {
                                TikaContent tc = tikaParser.parse(inputStream, contentHandler);
                                TikaMetadata tm = tc.getMetadata();
                                if (tm != null) {
                                    for (String name : tm.getNames()) {
                                        tm.getValues(name).stream().forEach((v) -> metadata.add(name, v));
                                    }
                                }
                            }
                        };
                    }
                };
                tikaConfiguration.setTikaConfig(config);
            }
            tikaConfiguration.setOperation(new URI(uri).getHost());

            return new TikaEndpoint(uri, this, tikaConfiguration);
        }
    }
}
