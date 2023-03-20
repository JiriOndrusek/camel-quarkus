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
package org.apache.camel.quarkus.component.cxf.soap.ssl.it;

import java.util.Map;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class CxfSoapSslRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureConverter")
    LoggingFeature loggingFeature;

    @Override
    public void configure() {

        from("direct:sslInvoker")
                .process(exchange -> {
                    Map<String, Object> headers = exchange.getIn().getHeaders();
                    headers.put("address", getServerUrl() + "/soapservice/Ssl/RouterPort");
                })
                .toD("cxf:bean:soapConverterEndpoint?address=${header.address}");

        from("cxf:bean:soapConverterRouterEndpoint")
                .process("responseProcessor");

    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapConverterEndpoint(SSLContextParameters mySslContext, DefaultHostnameVerifier defaultHostnameVerifier) {
        final CxfEndpoint result = new CxfEndpoint();
        result.getFeatures().add(loggingFeature);
        result.setServiceClass(GreeterService.class);
        result.setAddress("/Ssl/RouterPort");
        result.setSslContextParameters(mySslContext());
        result.setHostnameVerifier(defaultHostnameVerifier);
        return result;
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapConverterRouterEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.getFeatures().add(loggingFeature);
        result.setServiceClass(GreeterService.class);
        result.setAddress("/Ssl/RouterPort");
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureConverter")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @ApplicationScoped
    @Named("responseProcessor")
    static class PojoModeProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            String resp = new GreeterImpl().greetMe(exchange.getIn().getBody(String.class));
            exchange.getIn().setBody(resp);
        }

    }

    private static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST)
                ? config.getValue("quarkus.http.test-ssl-port", Integer.class)
                : config.getValue("quarkus.http.ssl-port", Integer.class);
        return String.format("https://localhost:%d", port);
    }

    @Produces
    @ApplicationScoped
    @Named("mySslContext")
    SSLContextParameters mySslContext() {
        SSLContextParameters sslContext = new SSLContextParameters();
        TrustManagersParameters trustManager = new TrustManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setType("JKS");
        keyStore.setPassword("changeit");
        keyStore.setResource("/ssl/truststore-client.jks");
        trustManager.setKeyStore(keyStore);
        sslContext.setTrustManagers(trustManager);
        return sslContext;
    }

    @Produces
    @ApplicationScoped
    @Named("defaultHostnameVerifier")
    DefaultHostnameVerifier defaultHostnameVerifier() {
        return new DefaultHostnameVerifier();
    }

}
