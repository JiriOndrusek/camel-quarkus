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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;
import org.apache.camel.component.splunk.SplunkEndpoint;
import org.apache.camel.component.splunk.SplunkProducer;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_HOST)
    String host;
    //    String host = "localhost";

    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_PORT)
    Integer port;
    //    Integer port = 32790;

    @ConfigProperty(name = SplunkConstants.PARAM_TCP_PORT)
    Integer tcpPort;
    //        Integer tcpPort = 32789;

    @Inject
    CamelContext camelContext;

    @Named
    SplunkComponent splunk() {
        SplunkComponent component = new SplunkComponent();
        component.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());
        component.setSslContextParameters(createServerSSLContextParameters());
        return component;
    }

    @Path("/reinitializeComponent")
    @GET
    public void reinitializeComponent() {
        //        ensureSslComponentExistence();
        camelContext.removeComponent("splunk");
    }

    @Path("/ssl/results/{name}")
    @POST
    public String resultsSsl(@PathParam("name") String mapName) {
        ensureSslComponentExistence();
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
        ensureSslComponentExistence();
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
    static SSLContextParameters createServerSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("password");
        keyStore.setResource("/certs/splunk-truststore.p12");
        keyManagersParameters.setKeyPassword("password");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setSecureSocketProtocol("TLSv1.2");

        return sslContextParameters;
    }

    //todo register in better way
    private void ensureSslComponentExistence() {
        //        if(camelContext.getComponent("splunk-ssl") == null) {
        //            SplunkComponent component = new SslSplunkComponent();
        //            component.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());
        //            camelContext.addComponent("splunk-ssl", component);
        //        }
    }

    static final class SslSplunkComponent extends SplunkComponent {

        public SslSplunkComponent() {
            super();

            setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());

        }

        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            SplunkConfiguration configuration = getSplunkConfigurationFactory().parseMap(parameters);
            SplunkEndpoint answer = new SslSplunkEndpoint(uri, this, configuration);
            setProperties(answer, parameters);
            configuration.setName(remaining);
            answer.setSslContextParameters(createServerSSLContextParameters());
            return answer;
        }

    }

    static final class SslSplunkEndpoint extends SplunkEndpoint {
        private static final Pattern SPLUNK_SCHEMA_PATTERN = Pattern.compile("splunk-ssl:(//)*");
        private static final Pattern SPLUNK_OPTIONS_PATTER = Pattern.compile("\\?.*");

        public SslSplunkEndpoint(String uri, SplunkComponent component, SplunkConfiguration configuration) {
            super(uri, component, configuration);
        }

        @Override
        public Producer createProducer() throws Exception {
            String[] uriSplit = splitUri(getEndpointUri());
            if (uriSplit.length > 0) {
                ProducerType producerType = ProducerType.fromUri(uriSplit[0]);
                return new SplunkProducer(this, producerType);
            }
            throw new IllegalArgumentException(
                    "Cannot create any producer with uri " + getEndpointUri()
                            + ". A producer type was not provided (or an incorrect pairing was used).");
        }

        private static String[] splitUri(String uri) {
            uri = SPLUNK_SCHEMA_PATTERN.matcher(uri).replaceAll("");
            uri = SPLUNK_OPTIONS_PATTER.matcher(uri).replaceAll("");

            return uri.split("/");
        }

    }
}
