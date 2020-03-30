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

import java.net.URI;
import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.camel.component.tika.TikaProducer;
import org.apache.tika.config.TikaConfig;

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
            TikaConfiguration tikaConfiguration = new TikaConfiguration();
            setProperties(tikaConfiguration, parameters);
            TikaConfig config = resolveAndRemoveReferenceParameter(parameters, TIKA_CONFIG, TikaConfig.class);
            if (config != null) {
                tikaConfiguration.setTikaConfig(config);
            }
            tikaConfiguration.setOperation(new URI(uri).getHost());

            TikaEndpoint endpoint = new QuarkusTikaEndpoint(uri, this, tikaConfiguration);
            return endpoint;
        }
    }

    static class QuarkusTikaEndpoint extends TikaEndpoint {

        public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration) {
            super(endpointUri, component, tikaConfiguration);
        }

        @Override
        public Producer createProducer() throws Exception {

            //TODO get tikaParse intialized by quarkus.tika
            return new QuarkusTikaProducer(this);
        }
    }

    static class QuarkusTikaProducer extends TikaProducer {

        public QuarkusTikaProducer(TikaEndpoint endpoint) {
            super(endpoint);
        }
    }
}
