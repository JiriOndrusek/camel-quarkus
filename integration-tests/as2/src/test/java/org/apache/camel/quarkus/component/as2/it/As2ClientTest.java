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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.entity.AS2MessageDispositionNotificationEntity;
import org.apache.camel.quarkus.component.as2.it.util.As2ClientHelper;
import org.apache.camel.quarkus.component.as2.it.util.As2ServerReceiver;
import org.apache.camel.support.PropertyBindingSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTestResource(As2TestResource.class)
@QuarkusTest
public class As2ClientTest {
    
    public static String PORT_PARAMETER = As2ClientTest.class.getSimpleName() + "-port";

    private static final String REQUEST_URI = "/";
    private static final String SUBJECT = "Test Case";
    private static final String AS2_NAME = "878051556";
    private static final String FROM = "mrAS@example.org";
    private static final String EDI_MESSAGE_CONTENT_TRANSFER_ENCODING = "7bit";

    @Inject
    private CamelContext context;

    @BeforeAll
    public static void beforeAll() throws Exception {
//   todo jondruse     As2ServerReceiver.start();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
//        //prepare component in camel context
//        // create AS2 component configuration
//        Map<String, Object> options = new HashMap<>();
//        //host name of host messages sent to
//        options.put("targetHostname","localhost");
//        //port number of host messages sent to
//        options.put("targetPortNumber", System.getProperty(PORT_PARAMETER));
//        //fully qualified domain name used in Message-Id message header.
//        options.put("clientFqdn", "example.com");
//        //port number listened on
//        options.put("serverPortNumber", System.getProperty(PORT_PARAMETER));
//
//        final AS2Configuration configuration = new AS2Configuration();
//        PropertyBindingSupport.bindProperties(context, configuration, options);
//
//        // add AS2Component to Camel context
//        final AS2Component component = new AS2Component(context);
//        component.setConfiguration(configuration);
//        context.addComponent("as2", component);

        //setup client
        new As2ClientHelper().setup();
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

        Result result = executeRequest(headers);

        assertNotNull(result, "Response entity");
        assertEquals(2, result.getPartsCount(), "Unexpected number of body parts in report");
        assertEquals(AS2MessageDispositionNotificationEntity.class.getSimpleName(), result.getSecondPartClassName(), "Unexpected type of As2Entity");
    }

    private Result executeRequest(Map<String, Object> headers) throws Exception {
        return RestAssured.given() //
                .contentType(ContentType.JSON)
                .body(new Headers().withHeaders(headers))
                .post("/as2/client") //
                .then()
                .statusCode(200)
                .extract().body().as(Result.class);
    }
}
