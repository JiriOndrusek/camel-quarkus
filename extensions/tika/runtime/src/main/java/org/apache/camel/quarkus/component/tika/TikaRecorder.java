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

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TikaRecorder {

    //    public RuntimeValue<TikaComponent> createTikaComponent(BeanContainer container) {
    //        return new RuntimeValue<>(new QuarkusTikaComponent(container.beanInstance(TikaParserProducer.class)));
    //    }
    //
    //    @org.apache.camel.spi.annotations.Component("tika")
    //    static class QuarkusTikaComponent extends TikaComponent {
    //
    //        private final TikaParserProducer tikaParserProducer;
    //
    //        public QuarkusTikaComponent(TikaParserProducer tikaParserProducer) {
    //            this.tikaParserProducer = tikaParserProducer;
    //        }
    //
    //        @Override
    //        protected TikaEndpoint createEndpoint(String uri, TikaConfiguration tikaConfiguration) {
    //            return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, tikaParserProducer);
    //        }
    //    }
    //
    //    static class QuarkusTikaEndpoint extends TikaEndpoint {
    //
    //        private final TikaParserProducer tikaParserProducer;
    //
    //        public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
    //                TikaParserProducer tikaParserProducer) {
    //            super(endpointUri, component, tikaConfiguration);
    //            this.tikaParserProducer = tikaParserProducer;
    //        }
    //
    //        @Override
    //        public Producer createProducer() throws Exception {
    //            TikaParser tikaParser = tikaParserProducer.tikaParser();
    //            return new TikaProducer(this, new Parser() {
    //                @Override
    //                public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
    //                    return Collections.emptySet();
    //                }
    //
    //                @Override
    //                public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata,
    //                        ParseContext parseContext) throws IOException, SAXException, TikaException {
    //                    TikaContent tc = tikaParser.parse(inputStream, contentHandler);
    //                    TikaMetadata tm = tc.getMetadata();
    //                    if (tm != null) {
    //                        for (String name : tm.getNames()) {
    //                            tm.getValues(name).stream().forEach((v) -> metadata.add(name, v));
    //                        }
    //                    }
    //                }
    //            });
    //        }
    //    }

}
