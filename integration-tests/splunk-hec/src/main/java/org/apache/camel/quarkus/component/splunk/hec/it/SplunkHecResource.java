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
package org.apache.camel.quarkus.component.splunk.hec.it;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunkhec.SplunkHECConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/splunk-hec")
@ApplicationScoped
public class SplunkHecResource {

    @Inject
    ProducerTemplate producer;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_PORT)
    Integer hecPort;

    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_HOST)
    String host;

    @ConfigProperty(name = SplunkConstants.PARAM_TEST_INDEX)
    String index;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_TOKEN)
    String token;

    @Inject
    CamelContext camelContext;
    //
    //    @Named("splunk-hec")
    //    SplunkHECComponent splunkHec() {
    //        final SplunkHECComponent splunkHec = new SplunkHECComponent();
    //        splunkHec.setCamelContext(camelContext);
    //        splunkHec.setUseGlobalSslContextParameters(true);
    //        splunkHec.setSslContextParameters(createServerSSLContextParameters());
    //        return splunkHec;
    //    }

    @Path("/send")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String send(String data, @QueryParam("indexTime") Long indexTime) {
        String url = String.format(
                "splunk-hec:%s:%s?token=%s&sslContextParameters=#sslContextParameters&skipTlsVerify=false&https=true&index=%s",
                host, hecPort, token,
                index);
        return producer.requestBodyAndHeader(url, data, SplunkHECConstants.INDEX_TIME, indexTime, String.class);
    }

    @Path("/sendHttp")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String sendHttp(String data, @QueryParam("indexTime") Long indexTime) {
        String url = String.format("splunk-hec:%s:%s?token=%s&skipTlsVerify=true&https=false&index=%s", host, hecPort, token,
                index);
        return producer.requestBodyAndHeader(url, data, SplunkHECConstants.INDEX_TIME, indexTime, String.class);
    }

    /**
     * Creates SSL Context Parameters for the server
     *
     * @return
     */
    @Named("sslContextParameters")
    public SSLContextParameters createServerSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        //        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        //        KeyStoreParameters keyStore = new KeyStoreParameters();
        //        keyStore.setPassword("password");
        //        keyStore.setResource("/keytool/splunkca.jks");
        //        keyManagersParameters.setKeyPassword("password");
        //        keyManagersParameters.setKeyStore(keyStore);
        //        sslContextParameters.setKeyManagers(keyManagersParameters);

//        //works
//        TrustManagersParameters trustManagersParameters = new TrustManagersParameters() {
//            @Override
//            public TrustManager[] createTrustManagers() {
//                return new TrustManager[] { new X509TrustManager() {
//                    @Override
//                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//
//                    }
//
//                    @Override
//                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//
//                    }
//
//                    @Override
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return new X509Certificate[0];
//                    }
//                } };
//            }
//        };

        //        works

        //        // Create KeyStoreParameters to load the CA certificate
        //        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        //        keyStoreParameters.setResource(getClass().getResource("/keytool/splunkca.jks").getFile()); // Path to the CA certificate file
        //        keyStoreParameters.setPassword("password"); // Password for the keystore if any
        //
        //        // Create TrustManagersParameters to use the CA certificate
        //        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        //        trustManagersParameters.setKeyStore(keyStoreParameters);
        //
        //        // Create and configure SSLContextParameters
        //        SSLContextParameters sslContextParameters = new SSLContextParameters();
        //        sslContextParameters.setTrustManagers(trustManagersParameters);

//
        sslContextParameters = new SSLContextParameters() {
            @Override
            public SSLContext createSSLContext(CamelContext camelContext) throws GeneralSecurityException, IOException {

                try {
                    return bealdung();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }


            }
        };

        return sslContextParameters;

//        return createServerSSLContextParameters2();
    }
//
//    /**
//     * Creates SSL Context Parameters for the server
//     *
//     * @return
//     */
//    @Named("wrongSslContextParameters")
//    public SSLContextParameters createWrongServerSSLContextParameters() {
//        SSLContextParameters sslContextParameters = new SSLContextParameters();
//
//        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
//        KeyStoreParameters keyStore = new KeyStoreParameters();
//        keyStore.setPassword("password");
//        keyStore.setResource("/wrong-splunkca.jks");
//        keyManagersParameters.setKeyPassword("password");
//        keyManagersParameters.setKeyStore(keyStore);
//        sslContextParameters.setKeyManagers(keyManagersParameters);
//
//        return sslContextParameters;
//    }


    /**
     * Creates SSL Context Parameters for the component
     *
     * @return
     */
    SSLContextParameters createServerSSLContextParameters2() {
//
//        if (sslContextParameters != null) {
//            return sslContextParameters;
//        }

        //        //create a truststore from the pems (exported from the container)
        //        try {
        //            createTruststore(SplunkResource.class.getResource("/server_from_container.pem").getFile(),
        //                    SplunkResource.class.getResource("/fromBrowser.cert").getFile(),
        //                    java.nio.file.Path.of(SplunkResource.class.getResource("/").getFile()).resolve("truststore.jks").toFile()
        //                            .getAbsolutePath());
        //        } catch (Exception e) {
        //            throw new RuntimeException(e);
        //        }

        SSLContextParameters sslContextParameters = new SSLContextParameters();

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("password");
        keyStore.setResource("/keytool/splunkca.jks");
        keyManagersParameters.setKeyPassword("password");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setSecureSocketProtocol("TLSv1.2");

        return sslContextParameters;
    }

    public static void createTruststore(String serverPemPath, String caCertPemPath, String truststorePath) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, "password".toCharArray());

        //        // Load server certificate
        //        try (FileInputStream fis = new FileInputStream(serverPemPath)) {
        //            X509Certificate serverCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        //            trustStore.setCertificateEntry("server", serverCert);
        //        }

        // Load CA certificate
        try (FileInputStream fis = new FileInputStream(caCertPemPath)) {
            X509Certificate caCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
            trustStore.setCertificateEntry("localhost", caCert);
        }

        // Save the truststore
        try (FileOutputStream fos = new FileOutputStream(truststorePath)) {
            trustStore.store(fos, "password".toCharArray());
        }
    }

    public static SSLContext createSSLContext(String trustStorePath, String trustStorePassword) throws Exception {
        // Load the custom truststore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
        }

        // Initialize the TrustManagerFactory with the loaded truststore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // Create and initialize the SSLContext with the trust managers from the truststore
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }

    static void intiSsl() throws Exception {
        // Load the CA certificate into a KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        try (InputStream caCertInputStream = new FileInputStream(
                SplunkHecResource.class.getResource("/keytool/splunkca.pem").getFile())) {
            java.security.cert.Certificate caCert = java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(caCertInputStream);
            keyStore.setCertificateEntry("myCaCert", caCert);
        }

        // Initialize the TrustManagerFactory with the KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Initialize the SSLContext with the TrustManagerFactory
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

        // Set the default SSLContext to use our custom SSLContext
        SSLContext.setDefault(sslContext);

        // Now, the default SSLContext for the JVM will use the custom truststore
        System.out.println("Custom SSLContext with truststore set successfully.");
    }

    static SSLContext bealdung() throws Exception {
        ///bealdung https://www.baeldung.com/java-custom-truststore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        X509TrustManager defaultX509CertificateTrustManager = null;
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                defaultX509CertificateTrustManager = x509TrustManager;
                break;
            }
        }

        try (FileInputStream myKeys = new FileInputStream(
                SplunkHecResource.class.getResource("/keytool/splunkca.jks").getFile())) {
            KeyStore myTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            myTrustStore.load(myKeys, "password".toCharArray());
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(myTrustStore);

            X509TrustManager myTrustManager = null;
            for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509TrustManager) {
                    myTrustManager = x509TrustManager;
                    break;
                }
            }

            X509TrustManager finalDefaultTm = defaultX509CertificateTrustManager;
            X509TrustManager finalMyTm = myTrustManager;

            X509TrustManager wrapper = new X509TrustManager() {
                private X509Certificate[] mergeCertificates() {
                    ArrayList<X509Certificate> resultingCerts = new ArrayList<>();
                    resultingCerts.addAll(Arrays.asList(finalDefaultTm.getAcceptedIssuers()));
                    resultingCerts.addAll(Arrays.asList(finalMyTm.getAcceptedIssuers()));
                    return resultingCerts.toArray(new X509Certificate[resultingCerts.size()]);
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return mergeCertificates();
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        finalMyTm.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        finalDefaultTm.checkServerTrusted(chain, authType);
                    }
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    finalDefaultTm.checkClientTrusted(mergeCertificates(), authType);
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { wrapper }, null);
            return context;
        }

    }

    static void splunk_for_java() throws Exception {

        X509Certificate caCert;
        try (InputStream caCertInputStream = new FileInputStream(
                SplunkHecResource.class.getResource("/keytool/splunkca.pem").getFile())) {
            caCert = (X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate(caCertInputStream);
        }
        // Create an SSLSocketFactory configured to use TLS only
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] byPassTrustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[] { caCert };
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                }
        };

        sslContext.init(null, byPassTrustManagers, new SecureRandom());
        SSLContext.setDefault(sslContext);
    }

}
