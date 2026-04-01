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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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

    @Produces
    @Singleton
    @Named("dilithiumKeyPair")
    public KeyPair dilithiumKeyPair() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("Dilithium2", "BCPQC");

        byte[] publicKeyBytes = Files.readAllBytes(
                Paths.get("target/certs/dilithium-public.key"));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        byte[] privateKeyBytes = Files.readAllBytes(
                Paths.get("target/certs/dilithium-private.key"));
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        return new KeyPair(publicKey, privateKey);
    }
}
