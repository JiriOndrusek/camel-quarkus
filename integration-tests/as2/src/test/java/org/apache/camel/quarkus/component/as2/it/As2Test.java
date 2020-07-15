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
package org.apache.camel.quarkus.component.as2.it;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2Constants;
import org.apache.camel.component.as2.api.AS2Header;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2MimeType;
import org.apache.camel.component.as2.api.AS2ServerConnection;
import org.apache.camel.component.as2.api.AS2ServerManager;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.AS2SignedDataGenerator;
import org.apache.camel.component.as2.api.entity.ApplicationEDIEntity;
import org.apache.camel.component.as2.api.util.HttpMessageUtils;
import org.apache.camel.component.as2.internal.AS2ApiCollection;
import org.apache.camel.component.as2.internal.AS2ClientManagerApiMethod;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpDateGenerator;
import org.apache.http.protocol.HttpRequestHandler;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class As2Test {

    //    @Test
    //    public void test() {
    //        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");
    //        RestAssured.given() //
    //                .contentType(ContentType.TEXT)
    //                .body(msg)
    //                .post("/as2/post") //
    //                .then()
    //                .statusCode(201);
    //
    //        Assertions.fail("Add some assertions to " + getClass().getName());
    //
    //        RestAssured.get("/as2/get")
    //                .then()
    //                .statusCode(200);
    //    }

    private static final String TEST_OPTIONS_PROPERTIES = "/test-options.properties";

    private static final Logger LOG = LoggerFactory.getLogger(As2Test.class);
    private static final String PATH_PREFIX = AS2ApiCollection.getCollection().getApiName(AS2ClientManagerApiMethod.class)
            .getName();

    private static final String SERVER_FQDN = "server.example.com";
    private static final String ORIGIN_SERVER_NAME = "AS2ClientManagerIntegrationTest Server";
    private static final String AS2_VERSION = "1.1";
    private static final String REQUEST_URI = "/";
    private static final String SUBJECT = "Test Case";
    private static final String AS2_NAME = "878051556";
    private static final String FROM = "mrAS@example.org";

    private static final String MDN_FROM = "as2Test@server.example.com";
    private static final String MDN_SUBJECT_PREFIX = "MDN Response:";

    private static final String EDI_MESSAGE_CONTENT_TRANSFER_ENCODING = "7bit";
    private static final String EXPECTED_AS2_VERSION = AS2_VERSION;
    private static final String EXPECTED_MDN_SUBJECT = MDN_SUBJECT_PREFIX + SUBJECT;
    private static final String[] SIGNED_RECEIPT_MIC_ALGORITHMS = new String[] { "sha1", "md5" };
    private static final String DISPOSITION_NOTIFICATION_OPTIONS = "signed-receipt-protocol=optional,pkcs7-signature; signed-receipt-micalg=optional,sha1";
    private static final int PARTNER_TARGET_PORT = 8888;
    private static final int MDN_TARGET_PORT = AvailablePortFinder.getNextAvailable();
    private static final String RECIPIENT_DELIVERY_ADDRESS = "http://localhost:" + MDN_TARGET_PORT + "/handle-receipts";
    private static final String REPORTING_UA = "Server Responding with MDN";

    private static AS2ServerConnection serverConnection;
    private static KeyPair serverSigningKP;
    private static List<X509Certificate> serverCertList;
    private static RequestHandler requestHandler;

    private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();

    @Inject
    private CamelContext context;

    private KeyPair issueKP;
    private X509Certificate issueCert;

    private KeyPair signingKP;
    private KeyPair decryptingKP;
    private X509Certificate signingCert;
    private List<X509Certificate> certList;
    private AS2SignedDataGenerator gen;

    @BeforeEach
    public void before() throws IOException {

        // read AS2 component configuration from TEST_OPTIONS_PROPERTIES
        final Properties properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream(TEST_OPTIONS_PROPERTIES));
        } catch (Exception e) {
            throw new IOException(String.format("%s could not be loaded: %s", TEST_OPTIONS_PROPERTIES, e.getMessage()),
                    e);
        }

        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            options.put(entry.getKey().toString(), entry.getValue());
        }

        final AS2Configuration configuration = new AS2Configuration();
        PropertyBindingSupport.bindProperties(context, configuration, options);

        // add AS2Component to Camel context
        final AS2Component component = new AS2Component(context);
        component.setConfiguration(configuration);
        context.addComponent("as2", component);
    }

    @BeforeAll
    public static void setupTest() throws Exception {
        setupServerKeysAndCertificates();
        receiveTestMessages();
    }

    //    @Override
    @BeforeEach
    public void setUp() throws Exception {
        //        super.setUp();
        Security.addProvider(new BouncyCastleProvider());

        setupKeysAndCertificates();

        // Create and populate certificate store.
        JcaCertStore certs = new JcaCertStore(certList);

        // Create capabilities vector
        SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
        capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
        capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
        capabilities.addCapability(SMIMECapability.dES_CBC);

        // Create signing attributes
        ASN1EncodableVector attributes = new ASN1EncodableVector();
        attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(
                new IssuerAndSerialNumber(new X500Name(signingCert.getIssuerDN().getName()), signingCert.getSerialNumber())));
        attributes.add(new SMIMECapabilitiesAttribute(capabilities));

        for (String signingAlgorithmName : AS2SignedDataGenerator
                .getSupportedSignatureAlgorithmNamesForKey(signingKP.getPrivate())) {
            try {
                this.gen = new AS2SignedDataGenerator();
                this.gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC")
                        .setSignedAttributeGenerator(new AttributeTable(attributes))
                        .build(signingAlgorithmName, signingKP.getPrivate(), signingCert));
                this.gen.addCertificates(certs);
                break;
            } catch (Exception e) {
                this.gen = null;
                continue;
            }
        }

        if (this.gen == null) {
            throw new Exception("failed to create signing generator");
        }
    }

    @Test
    public void plainMessageSendTest() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        // parameter type is String
        headers.put("CamelAS2.requestUri", REQUEST_URI);
        // parameter type is String
        headers.put("CamelAS2.subject", SUBJECT);
        // parameter type is String
        headers.put("CamelAS2.from", FROM);
        // parameter type is String
        headers.put("CamelAS2.as2From", AS2_NAME);
        // parameter type is String
        headers.put("CamelAS2.as2To", AS2_NAME);
        // parameter type is org.apache.camel.component.as2.api.AS2MessageStructure
        headers.put("CamelAS2.as2MessageStructure", AS2MessageStructure.PLAIN);
        // parameter type is org.apache.http.entity.ContentType
        headers.put("CamelAS2.ediMessageContentType",
                org.apache.http.entity.ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII));
        // parameter type is String
        headers.put("CamelAS2.ediMessageTransferEncoding", EDI_MESSAGE_CONTENT_TRANSFER_ENCODING);
        // parameter type is String
        headers.put("CamelAS2.dispositionNotificationTo", "mrAS2@example.com");

        //        Endpoint e = context.getEndpoint("as2://client/send?inBody=ediMessage");

        final Triple<Result, HttpRequest, HttpResponse> result = executeRequest(headers);
        Result responseEntity = result.getLeft();
        HttpRequest request = result.getMiddle();
        HttpResponse response = result.getRight();

        assertNotNull(result, "send result");
        LOG.debug("send: " + result);
        assertNotNull(request, "Request");
        assertTrue(request instanceof HttpEntityEnclosingRequest, "Request does not contain body");
        HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
        assertNotNull(entity, "Request body");
        assertTrue(entity instanceof ApplicationEDIEntity, "Request body does not contain EDI entity");
        String ediMessage = ((ApplicationEDIEntity) entity).getEdiMessage();
        assertEquals(As2Resource.EDI_MESSAGE.replaceAll("[\n\r]", ""), ediMessage.replaceAll("[\n\r]", ""),
                "EDI message is different");

        assertNotNull(response, "Response");
        assertTrue(HttpMessageUtils.getHeaderValue(response, AS2Header.CONTENT_TYPE).startsWith(AS2MimeType.MULTIPART_REPORT),
                "Unexpected response type");
        assertEquals(AS2Constants.MIME_VERSION, HttpMessageUtils.getHeaderValue(response, AS2Header.MIME_VERSION),
                "Unexpected mime version");
        assertEquals(EXPECTED_AS2_VERSION, HttpMessageUtils.getHeaderValue(response, AS2Header.AS2_VERSION),
                "Unexpected AS2 version");
        assertEquals(EXPECTED_MDN_SUBJECT, HttpMessageUtils.getHeaderValue(response, AS2Header.SUBJECT),
                "Unexpected MDN subject");
        assertEquals(MDN_FROM, HttpMessageUtils.getHeaderValue(response, AS2Header.FROM), "Unexpected MDN from");
        assertEquals(AS2_NAME, HttpMessageUtils.getHeaderValue(response, AS2Header.AS2_FROM), "Unexpected AS2 from");
        assertEquals(AS2_NAME, HttpMessageUtils.getHeaderValue(response, AS2Header.AS2_TO), "Unexpected AS2 to");
        assertNotNull(HttpMessageUtils.getHeaderValue(response, AS2Header.MESSAGE_ID), "Missing message id");

        assertNotNull(responseEntity, "Response entity");
        assertEquals(2, responseEntity.getPartsCount(), "Unexpected number of body parts in report");
    }

    private static void setupServerKeysAndCertificates() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = Utils.makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        serverSigningKP = kpg.generateKeyPair();
        X509Certificate signingCert = Utils.makeCertificate(
                serverSigningKP, signingDN, issueKP, issueDN);

        serverCertList = new ArrayList<>();

        serverCertList.add(signingCert);
        serverCertList.add(issueCert);
    }

    private void setupKeysAndCertificates() throws Exception {
        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        issueKP = kpg.generateKeyPair();
        issueCert = Utils.makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        signingKP = kpg.generateKeyPair();
        signingCert = Utils.makeCertificate(
                signingKP, signingDN, issueKP, issueDN);

        certList = new ArrayList<>();

        certList.add(signingCert);
        certList.add(issueCert);

        // keys used to encrypt/decrypt
        decryptingKP = signingKP;
    }

    private Triple<Result, HttpRequest, HttpResponse> executeRequest(Map<String, Object> headers) throws Exception {
        Point p = new Point();
        p.applyHeadersTypeSafe(headers);
        //        p.setMessageStructure((AS2MessageStructure) headers.get("CamelAS2.as2MessageStructure"));
        //        p.setMessageStructureKey("CamelAS2.as2MessageStructure");
        //        p.setContentTypeKey("CamelAS2.ediMessageContentType");
        //        p.setContentType((org.apache.http.entity.ContentType)headers.get(p.getContentTypeKey()));
        Result result = RestAssured.given() //
                .contentType(ContentType.JSON)
                .body(p)
                .post("/as2/post") //
                .then()
                .statusCode(201)
                .extract().body().as(Result.class);

        return new ImmutableTriple(/*responseEntity*/ result, requestHandler.getRequest(), requestHandler.getResponse());
    }

    private static void receiveTestMessages() throws IOException {
        serverConnection = new AS2ServerConnection(AS2_VERSION, ORIGIN_SERVER_NAME,
                SERVER_FQDN, PARTNER_TARGET_PORT, AS2SignatureAlgorithm.SHA256WITHRSA,
                serverCertList.toArray(new Certificate[0]), serverSigningKP.getPrivate(), serverSigningKP.getPrivate());
        requestHandler = new RequestHandler();
        serverConnection.listen("/", requestHandler);
    }

    public static class RequestHandler implements HttpRequestHandler {

        private HttpRequest request;
        private HttpResponse response;

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            LOG.info("Received test message: " + request);
            context.setAttribute(AS2ServerManager.FROM, MDN_FROM);
            context.setAttribute(AS2ServerManager.SUBJECT, MDN_SUBJECT_PREFIX);

            this.request = request;
            this.response = response;
        }

        public HttpRequest getRequest() {
            return request;
        }

        public HttpResponse getResponse() {
            return response;
        }
    }

}
