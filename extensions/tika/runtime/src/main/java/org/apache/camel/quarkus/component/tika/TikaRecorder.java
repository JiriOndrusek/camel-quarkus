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

import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tika.TikaParser;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.camel.component.tika.TikaProducer;

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
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            TikaEndpoint endpoint = new QuarkusTikaEndpoint(uri, this);
            return endpoint;
        }
    }

    static class QuarkusTikaEndpoint extends TikaEndpoint {

        public QuarkusTikaEndpoint(String endpointUri, Component component) {
            super(endpointUri, component, null);
        }

        @Override
        public Producer createProducer() throws Exception {
            TikaParser tikaParser = getCamelContext().getRegistry().findByType(TikaParser.class).iterator().next();
            //TODO get tikaParse intialized by quarkus.tika
            return new QuarkusTikaProducer(this, tikaParser);
        }
    }

    static class QuarkusTikaProducer extends TikaProducer {

        public QuarkusTikaProducer(TikaEndpoint endpoint, TikaParser tikaParser) {
            super(endpoint, tikaParser);
        }
    }
}
