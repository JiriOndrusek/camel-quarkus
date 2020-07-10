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
package org.apache.camel.quarkus.component.cassandraql;

import java.util.Map;

import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.component.cassandra.CassandraComponent;
import org.apache.camel.component.cassandra.CassandraEndpoint;

@Recorder
public class CassandraRecorder {

    public RuntimeValue<CassandraComponent> createCassandraComponent(BeanContainer container) {
        return new RuntimeValue<>(new QuarkusCassandraComponent(container.instance(QuarkusCqlSession.class)));
    }

    @org.apache.camel.spi.annotations.Component("cql")
    static class QuarkusCassandraComponent extends CassandraComponent {

        private final QuarkusCqlSession cqs;

        public QuarkusCassandraComponent(QuarkusCqlSession session) {
            this.cqs = session;
        }

        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            CassandraEndpoint endpoint = (CassandraEndpoint) super.createEndpoint(uri, remaining, parameters);

            //provide cassandra quarkus session
            endpoint.setSession(cqs);

            return endpoint;
        }
    }

}
