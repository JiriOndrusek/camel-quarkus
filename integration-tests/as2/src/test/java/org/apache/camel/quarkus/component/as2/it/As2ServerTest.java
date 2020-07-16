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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.AS2ClientManager;
import org.apache.camel.component.as2.api.AS2Header;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.entity.AS2MessageDispositionNotificationEntity;
import org.apache.camel.component.as2.api.entity.ApplicationEDIFACTEntity;
import org.apache.camel.component.as2.internal.AS2ApiCollection;
import org.apache.camel.component.as2.internal.AS2Constants;
import org.apache.camel.component.as2.internal.AS2ServerManagerApiMethod;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.as2.it.util.As2ClientHelper;
import org.apache.camel.quarkus.component.as2.it.util.As2ServerHelper;
import org.apache.camel.quarkus.component.as2.it.util.As2ServerReceiver;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@QuarkusTestResource(As2TestResource.class)
@QuarkusTest
public class As2ServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(As2ServerTest.class);
    private static final String PATH_PREFIX = AS2ApiCollection.getCollection().getApiName(AS2ServerManagerApiMethod.class).getName();

    private static final String METHOD = "POST";
    private static final String TARGET_HOST = "localhost";
    private static final String AS2_VERSION = "1.1";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String REQUEST_URI = "/";
    private static final String AS2_NAME = "878051556";
    private static final String SUBJECT = "Test Case";
    private static final String FROM = "mrAS@example.org";
    private static final String CLIENT_FQDN = "example.org";
    private static final String DISPOSITION_NOTIFICATION_TO = "mrAS@example.org";
    private static final String[] SIGNED_RECEIPT_MIC_ALGORITHMS = new String[] {"sha1", "md5"};

    @Inject
    private CamelContext context;

    private static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            + "UNH+00000000000117+INVOIC:D:97B:UN'\n"
            + "BGM+380+342459+9'\n"
            + "DTM+3:20060515:102'\n"
            + "RFF+ON:521052'\n"
            + "NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            + "NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            + "CUX+1:USD'\n"
            + "LIN+1++157870:IN'\n"
            + "IMD+F++:::WIDGET'\n"
            + "QTY+47:1020:EA'\n"
            + "ALI+US'\n"
            + "MOA+203:1202.58'\n"
            + "PRI+INV:1.179'\n"
            + "LIN+2++157871:IN'\n"
            + "IMD+F++:::DIFFERENT WIDGET'\n"
            + "QTY+47:20:EA'\n"
            + "ALI+JP'\n"
            + "MOA+203:410'\n"
            + "PRI+INV:20.5'\n"
            + "UNS+S'\n"
            + "MOA+39:2137.58'\n"
            + "ALC+C+ABG'\n"
            + "MOA+8:525'\n"
            + "UNT+23+00000000000117'\n"
            + "UNZ+1+00000000000778'";

    @BeforeAll
    public static void  setup() throws Exception {
        As2ServerHelper.setupSigningGenerator();
    }

    @Test
    public void receivePlainEDIMessageTest() throws Exception {
        AS2ClientConnection clientConnection = new AS2ClientConnection(AS2_VERSION, USER_AGENT, CLIENT_FQDN, TARGET_HOST, Integer.parseInt(System.getProperty(As2ClientTest.PORT_PARAMETER)));
        AS2ClientManager clientManager = new AS2ClientManager(clientConnection);

        clientManager.send(EDI_MESSAGE, REQUEST_URI, SUBJECT, FROM, AS2_NAME, AS2_NAME, AS2MessageStructure.PLAIN,
                ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII), null, null, null, null,
                null, DISPOSITION_NOTIFICATION_TO, SIGNED_RECEIPT_MIC_ALGORITHMS, null, null);

        MockEndpoint mockEndpoint = context.getEndpoint("mock:as2RcvMsgs", MockEndpoint.class);
        mockEndpoint.expectedMinimumMessageCount(1);
        mockEndpoint.setResultWaitTime(TimeUnit.MILLISECONDS.convert(30,  TimeUnit.SECONDS));
        mockEndpoint.assertIsSatisfied();

        final List<Exchange> exchanges = mockEndpoint.getExchanges();
        assertNotNull(exchanges, "listen result");
        assertFalse(exchanges.isEmpty(), "listen result");
        LOG.debug("poll result: " + exchanges);

        Exchange exchange = exchanges.get(0);
        Message message = exchange.getIn();
        assertNotNull(message, "exchange message");

        HttpCoreContext coreContext = exchange.getProperty(AS2Constants.AS2_INTERCHANGE, HttpCoreContext.class);
        assertNotNull(coreContext, "context");
        HttpRequest request = coreContext.getRequest();
        assertTrue(request instanceof BasicHttpEntityEnclosingRequest, "Request does not contain entity");

    }


}
