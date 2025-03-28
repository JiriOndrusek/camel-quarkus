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
package org.apache.camel.quarkus.component.cics.it;

import java.util.Map;

import com.redhat.camel.component.cics.CICSConstants;
import com.redhat.camel.component.cics.support.CICSDataExchangeType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@TestCertificates(certificates = {
        @Certificate(name = "localhost", formats = { Format.PKCS12 }, password = "changeit"),
        @Certificate(name = "wrong", formats = { Format.PKCS12 }, password = "changeit") })
@QuarkusTestResource(CicsTestResource.class)
@QuarkusTest
class CicsTest {

    int sslPort, tcpPort;
    String host;

    private static final Map<String, Object> containers = Map.of("FirstChar", "My First Container",
            "SecondByteArray", "My Second Container".getBytes());

    @Test
    public void commareaTcpNoFactory() {
        test("cics", CICSDataExchangeType.COMMAREA, "?host=%s&port=%d&protocol=%s".formatted(host, tcpPort, "tcp"));
    }

    @Test
    public void commareaTcpFactory() {
        test("cicsFactory", CICSDataExchangeType.COMMAREA, null);
    }

    @Test
    public void commareaTcpPooledFactory() {
        test("cicsPooledFactory", CICSDataExchangeType.COMMAREA, null);
    }
    //
    //    @Test
    //    public void commareaSslNoFactory() {
    //        test("ssl", CICSDataExchangeType.COMMAREA, "noFactory");
    //    }

    @Test
    public void commareaSslFactory() {
        test("cicsSslFactory", CICSDataExchangeType.COMMAREA, null);
    }

    @Test
    public void commareaSslPooledFactory() {
        test("cicsSslPooledFactory", CICSDataExchangeType.COMMAREA, null);
    }

    //    @Test
    //    public void commareaCorrectUsernameNoFactory() {
    //        test("auth", CICSDataExchangeType.COMMAREA, "noFactory", "&userId=cicstg&password=cicstg");
    //    }
    //
    //    @Test
    //    public void commareaWrongUsernameNoFactory() {
    //        test("auth", CICSDataExchangeType.COMMAREA, "noFactory", "&userId=cicstg&password=wrong");
    //    }

    //    private void test(String protocol, CICSDataExchangeType dataExchangeType, String factory) {
    //        test(protocol, dataExchangeType,  null);
    //    }

    private void test(String component, CICSDataExchangeType dataExchangeType, String urlSuffix) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .queryParam("urlSuffix", urlSuffix)
                .post("/cics/eciReady/" + component + "/" + dataExchangeType.name())
                .then()
                .statusCode(200)
                .body("body", Matchers.notNullValue())
                .body("$", Matchers.hasKey(CICSConstants.CICS_RETURN_CODE_HEADER))
                .body("$", Matchers.hasKey(CICSConstants.CICS_RETURN_CODE_STRING_HEADER))
                .body("$", Matchers.hasKey(CICSConstants.CICS_LUW_TOKEN_HEADER))
                .body("$", Matchers.hasKey(CICSConstants.CICS_EXTEND_MODE_HEADER))
                //validate output of the echo app for the commarea
                .body("body", Matchers.matchesRegex("\\d+/\\d+/\\d+.*"));
    }

    @Disabled
    @Test
    public void sslWrongCertificate() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/cics/eciReady/COMMAREA/ssl/sslWrongFactory")
                .then()
                .statusCode(200)
                .body("body", Matchers.emptyOrNullString());
    }

    /**
     * Channel should fail with a program deployed in the server (eciReady).
     * Test verifies, that channel type does not cause aby trouble for the native execution.
     */
    @Disabled
    @Test
    public void channelFail() {
        RestAssured.given()
                .body(containers)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/cics/eciReady/CHANNEL/tcp/factory")
                .then()
                .statusCode(200)
                //body stays the same (containers)
                .body("body", Matchers.containsString("FirstChar"))
                .body("body", Matchers.containsString("SecondByteArray"));
    }

}
