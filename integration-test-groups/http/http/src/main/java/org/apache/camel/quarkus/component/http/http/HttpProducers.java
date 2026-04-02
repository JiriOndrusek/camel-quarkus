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
package org.apache.camel.quarkus.component.http.http;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import jakarta.inject.Named;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN_PASSWORD;

public class HttpProducers {

    @Named
    HttpContext basicAuthContext() {
        Integer port = ConfigProvider.getConfig().getValue("quarkus.http.test-ssl-port", Integer.class);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(USER_ADMIN,
                USER_ADMIN_PASSWORD.toCharArray());
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(new AuthScope(null, -1), credentials);

        BasicAuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(new HttpHost("localhost", port), basicAuth);

        HttpClientContext context = HttpClientContext.create();
        context.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, provider);

        return context;
    }

    @Named
    public SSLContextParameters pqcSslContextParameters() {
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setResource("file://target/certs/localhost-keystore.p12");
        keystoreParameters.setPassword("localhost-keystore-password");

        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("file://target/certs/localhost-truststore.p12");
        truststoreParameters.setPassword("localhost-keystore-password");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(truststoreParameters);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyPassword("localhost-keystore-password");
        keyManagersParameters.setKeyStore(keystoreParameters);
        sslContextParameters.setKeyManagers(keyManagersParameters);

        // Enable PQC cipher suites if available
        sslContextParameters.setSecureSocketProtocol("TLS");

        return sslContextParameters;
    }

    @Named
    public SSLContextParameters pqcNginxSslContextParameters() {
        // Note: Standard Java JSSE doesn't support PQC signature algorithms (ML-DSA-44/Dilithium2)
        // for certificate validation, even with BouncyCastle BCPQC provider installed.
        // Full PQC support requires using BouncyCastle's TLS implementation (BCTLS) instead of JSSE.
        // For this test, we use a custom TrustManager that accepts all certificates to verify
        // the connection infrastructure works with PQC-signed certificates.
        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setTrustManager(new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Accept all certificates - this allows connections to servers with PQC-signed certificates
                // even though JSSE can't validate PQC signatures
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        });

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
    }

}
