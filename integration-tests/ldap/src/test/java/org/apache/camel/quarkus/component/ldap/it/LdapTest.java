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
package org.apache.camel.quarkus.component.ldap.it;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import jakarta.ws.rs.core.MediaType;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestCertificates(certificates = {
        @Certificate(name = "ldap", formats = {
                Format.PKCS12, Format.PEM }, password = "ldapPass") })
@QuarkusTest
class LdapTest {

    private static InMemoryDirectoryServer ldapServer = null;
    private static final String TRUSTSTORE_LOCATION = "certs/ldap-truststore.p12";
    private static final String KEYSTORE_LOCATION = "certs/ldap-keystore.p12";

    @BeforeAll
    public static void setUpLdapServer() throws Exception {

        // Create an LDAP server to handle unencrypted and TLS connections
        InMemoryDirectoryServerConfig dsConfig = new InMemoryDirectoryServerConfig("ou=system");
        InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig("ldap",
                InetAddress.getLoopbackAddress(), 0, null);

        //        Path keystorePath = Paths.get(KEYSTORE_LOCATION);
        //        Path truststorePath = Paths.get(TRUSTSTORE_LOCATION);
        //        if (!Files.isRegularFile(keystorePath)) {
        //            /* The test is run from a test-jar within Quarkus Platform, where the Ant script was not run
        //             * so let's copy the keystore from test-jar to the local folder */
        //            Files.createDirectories(keystorePath.getParent());
        //            try (InputStream in = LdapTest.class.getClassLoader().getResourceAsStream(keystorePath.getFileName().toString())) {
        //                Files.copy(in, keystorePath);
        //            }
        //            try (InputStream in = LdapTest.class.getClassLoader()
        //                    .getResourceAsStream(truststorePath.getFileName().toString())) {
        //                Files.copy(in, truststorePath);
        //            }
        //        }

        SSLUtil serverSSLUtil = new SSLUtil(
                new KeyStoreKeyManager(Thread.currentThread().getContextClassLoader().getResource(KEYSTORE_LOCATION).getFile(),
                        "ldapPass".toCharArray()),
                null);
        InMemoryListenerConfig sslListenerConfig = InMemoryListenerConfig.createLDAPSConfig("ldaps",
                InetAddress.getLoopbackAddress(), 0, serverSSLUtil.createSSLServerSocketFactory(),
                null);
        dsConfig.setListenerConfigs(listenerConfig, sslListenerConfig);
        ldapServer = new InMemoryDirectoryServer(dsConfig);

        // Load the LDIF file from the Camel LDAP tests
        LDIFReader ldifReader = new LDIFReader(
                LdapTest.class.getClassLoader().getResourceAsStream("LdapRouteTest.ldif"));
        ldapServer.importFromLDIF(true, ldifReader);
        ldapServer.startListening();
    }

    @AfterAll
    public static void tearDownLdapServer() {
        if (ldapServer != null) {
            ldapServer.close();
        }
    }

    /**
     * Calls a Camel route to search for LDAP entries where the uid is "tcruise".
     * The test is run in both SSL and non-SSL modes.
     *
     * @param  useSSL
     * @throws Exception
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void ldapSearchTest(boolean useSSL) throws Exception {
        configureResource(useSSL);

        TypeRef<List<Map<String, Object>>> typeRef = new TypeRef<>() {
        };
        List<Map<String, Object>> results = RestAssured.given()
                .queryParam("ldapQuery", "tcruise")
                .get("/ldap/search")
                .then()
                .statusCode(200)
                .extract().as(typeRef);

        assertEquals(1, results.size());
        assertEquals("Tom Cruise", results.get(0).get("cn"));
    }

    /**
     * Tests the escaping of search values using the
     * {@link org.apache.camel.component.ldap.LdapHelper} class.
     *
     * @throws Exception
     */
    @Test
    public void ldapHelperTest() throws Exception {
        configureResource(false);

        TypeRef<List<Map<String, Object>>> typeRef = new TypeRef<>() {
        };

        // Verfiy that calling the unsafe endpoint with a wildcard returns multiple results.
        List<Map<String, Object>> results = RestAssured.given()
                .queryParam("ldapQuery", "test*")
                .get("/ldap/search")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(3, results.size());
        assertEquals(List.of("test1", "test2", "testNoOU"),
                results.stream().map(r -> r.get("uid")).collect(Collectors.toList()));

        // Verify that the same query passed to the safeSearch returns no matching results.
        results = RestAssured.given()
                .queryParam("ldapQuery", "test*")
                .get("/ldap/safeSearch")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(0, results.size());

        // Verify that non-escaped queries also work with escaped search
        results = RestAssured.given()
                .queryParam("ldapQuery", "test1")
                .get("/ldap/safeSearch")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(1, results.size());
        assertEquals("test1", results.get(0).get("ou"));

    }

    /**
     * Configures the
     * {@link org.apache.camel.quarkus.component.ldap.it.LdapResource} by sending it
     * a Map of connection properties.
     *
     * @param  useSSL
     * @throws Exception
     */
    private void configureResource(boolean useSSL) throws Exception {
        // Configure the LdapResource with the connection details for the LDAP server
        String listenerName = useSSL ? "ldaps" : "ldap";
        Map<String, String> options = new HashMap<>();
        options.put("host", ldapServer.getListenAddress(listenerName).getHostAddress());
        options.put("port", String.valueOf(ldapServer.getListenPort(listenerName)));
        options.put("ssl", String.valueOf(useSSL));
        if (useSSL) {
            options.put("trustStore", TRUSTSTORE_LOCATION);
            options.put("trustStorePassword", "ldapPass");
        }

        RestAssured.given()
                .body(options)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/ldap/configure")
                .then()
                .statusCode(204);
    }
}
