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
package org.apache.camel.quarkus.component.splunk.it;

import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;

public class SplunkRoutes extends RouteBuilder {

    @Named("splunk-global-ssl")
    SplunkComponent splunkGlobalSsl() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final SplunkComponent splunkComponent = new SplunkComponent();
        splunkComponent.setCamelContext(getContext());
        splunkComponent.setUseGlobalSslContextParameters(true);
        splunkComponent.setSslContextParameters(createServerSSLContextParameters());
        return splunkComponent;
    }

    /**
     * Creates SSL Context Parameters for the server
     *
     * @return
     */
    public SSLContextParameters createServerSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("password");
        keyStore.setResource("truststore-from-server.jks");
        keyManagersParameters.setKeyPassword("password");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setSecureSocketProtocol("TLSv1.2");
        return sslContextParameters;
    }

    @Override
    public void configure() throws Exception {

    }
}
