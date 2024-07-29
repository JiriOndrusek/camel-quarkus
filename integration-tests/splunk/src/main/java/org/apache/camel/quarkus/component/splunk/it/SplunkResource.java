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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.jetbrains.annotations.NotNull;

@Path("/splunk")
@ApplicationScoped
public class SplunkResource {

    public static final String SAVED_SEARCH_NAME = "savedSearchForTest";
    public static final String SOURCE = "test";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    //    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_HOST)
    //    String host;
    String host = "localhost";

    //    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_PORT)
    //    Integer port;
    Integer port = 32786;

    //    @ConfigProperty(name = SplunkConstants.PARAM_TCP_PORT)
    //    Integer tcpPort;
    Integer tcpPort = 32785;

    @Inject
    CamelContext camelContext;

    SSLContextParameters sslContextParameters = null;

    @Named
    SplunkComponent splunk() {
        SplunkComponent component = new SplunkComponent();
        component.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());
        //        component.setSslContextParameters(createServerSSLContextParameters());
        return component;
    }

    @Path("/reinitializeComponent")
    @GET
    public void reinitializeComponent() {
        createServerSSLContextParameters();
        camelContext.removeComponent("splunk");
    }

    @Path("/ssl/results/{name}")
    @POST
    public String resultsSsl(@PathParam("name") String mapName) {
        return results(true, mapName);
    }

    @Path("/results/{name}")
    @POST
    public String results(@PathParam("name") String mapName) {
        return results(false, mapName);
    }

    private String results(boolean ssl, String mapName) {
        String url;
        int count = 3;

        if ("savedSearch".equals(mapName)) {
            url = String.format(
                    "%s://savedsearch?username=admin&password=changeit&scheme=%s&host=%s&port=%d&delay=500&initEarliestTime=-10m&savedsearch=%s",
                    getComponent(ssl), ssl ? "https" : "http", host, port, SAVED_SEARCH_NAME);
        } else if ("normalSearch".equals(mapName)) {
            url = String.format(
                    "%s://normal?username=admin&password=changeit&scheme=%s&host=%s&port=%d&delay=5000&initEarliestTime=-10s&search="
                            + "search sourcetype=\"SUBMIT\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                    getComponent(ssl), ssl ? "https" : "http", host, port);
        } else {
            url = String.format(
                    "%s://realtime?username=admin&password=changeit&scheme=%s&host=%s&port=%d&delay=3000&initEarliestTime=rt-10s&latestTime=RAW(rt+40s)&search="
                            + "search sourcetype=\"STREAM\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                    getComponent(ssl), ssl ? "https" : "http", host, port,
                    ProducerType.STREAM.name());
        }

        List<SplunkEvent> events = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            SplunkEvent se = consumerTemplate.receiveBody(url, 5000, SplunkEvent.class);
            if (se == null) {
                break;
            }
            events.add(se);
        }
        List result = events.stream()
                .map(m -> {
                    if (m == null) {
                        return "null";
                    }
                    return m.getEventData().get("_raw");
                })
                .collect(Collectors.toList());
        return result.toString();
    }

    private static @NotNull String getComponent(boolean ssl) {
        String component = ssl ? "splunk" : "splunk";
        return component;
    }

    @Path("/ssl/write/{producerType}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response writeSsl(Map<String, String> message,
            @PathParam("producerType") String producerType,
            @QueryParam("index") String index) throws URISyntaxException {
        return write(true, message, producerType, index);
    }

    @Path("/write/{producerType}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response write(Map<String, String> message,
            @PathParam("producerType") String producerType,
            @QueryParam("index") String index) throws URISyntaxException {
        return write(false, message, producerType, index);
    }

    private Response write(boolean ssl, Map<String, String> message,
            String producerType,
            String index) throws URISyntaxException {

        if (message.containsKey("_rawData")) {
            return writeRaw(ssl, message.get("_rawData"), producerType, index);
        }

        SplunkEvent se = new SplunkEvent();
        for (Map.Entry<String, String> e : message.entrySet()) {
            se.addPair(e.getKey(), e.getValue());
        }

        return writeRaw(ssl, se, producerType, index);
    }

    private Response writeRaw(boolean ssl, Object message,
            String producerType,
            String index) throws URISyntaxException {

        String url;
        if (ProducerType.TCP == ProducerType.valueOf(producerType)) {
            url = String.format(
                    "%s:%s?raw=%b&username=admin&password=changeit&scheme=%s&host=%s&port=%d&index=%s&sourceType=%s&source=%s&tcpReceiverLocalPort=%d&tcpReceiverPort=%d",
                    getComponent(ssl), producerType.toLowerCase(), !(message instanceof SplunkEvent), ssl ? "https" : "http",
                    host, port, index,
                    producerType,
                    SOURCE,
                    SplunkConstants.TCP_PORT, tcpPort);

        } else {
            url = String.format(
                    "%s:%s?raw=%b&username=admin&password=changeit&scheme=%s&host=%s&port=%d&index=%s&sourceType=%s&source=%s",
                    getComponent(ssl), producerType.toLowerCase(), !(message instanceof SplunkEvent), ssl ? "https" : "http",
                    host,
                    port, index,
                    producerType,
                    SOURCE);
        }
        final String response = producerTemplate.requestBody(url, message, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    /**
     * Creates SSL Context Parameters for the component
     *
     * @return
     */
    SSLContextParameters createServerSSLContextParameters() {

        if (sslContextParameters != null) {
            return sslContextParameters;
        }

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
        keyStore.setResource("/keytool/truststore-server.jks");
        keyManagersParameters.setKeyPassword("password");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setSecureSocketProtocol("TLSv1.2");

        //test todo, remove
        try {
            //
            //            SSLContext sslContext = createSSLContext(getClass().getResource("/keytool/truststore-server.jks").getFile(),
            //                    "password");
            //            SSLContext.setDefault(sslContextParameters.createSSLContext(camelContext));
            //                        SSLContext.setDefault(sslContext);

            //            intiSsl();
            //            bealdung();
            splunk_for_java();
        } catch (GeneralSecurityException e) {

            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.sslContextParameters = sslContextParameters;
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
                SplunkResource.class.getResource("/keytool/splunkca.pem").getFile())) {
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

    static void bealdung() throws Exception {
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
                SplunkResource.class.getResource("/keytool/truststore-server.jks").getFile())) {
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
            SSLContext.setDefault(context);
        }

    }

    static void splunk_for_java() throws Exception {

        X509Certificate caCert;
        try (InputStream caCertInputStream = new FileInputStream(
                SplunkResource.class.getResource("/keytool/splunkca.pem").getFile())) {
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
