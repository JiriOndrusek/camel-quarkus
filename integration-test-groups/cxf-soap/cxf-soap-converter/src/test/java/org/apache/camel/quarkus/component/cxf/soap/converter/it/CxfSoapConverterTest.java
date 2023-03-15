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
package org.apache.camel.quarkus.component.cxf.soap.converter.it;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CxfSoapConverterTest {

    // Test is ported from PayLoadConvertToPOJOTest in Camel-spring-boot/components-starter/camel-cxf-soap-starter
    @Test
    public void pojoConvertTest() throws Exception {
        RestAssured.given()
                .body("abc")
                .post("/cxf-soap/converter/pojo/")
                .then()
                .statusCode(201)
                .body(equalTo("abc2"));
    }

    //test is ported frim CxfConsumerPayLoadConverterTest in Camel-spring-boot/components-starter/camel-cxf-soap-starter
    @Test
    public void consumerConvertTest() throws Exception {
        final EchoService echo = QuarkusCxfClientTestUtil.getClient(EchoService.class,
                "/soapservice/PayLoadConvert/RouterPort2");
        Assertions.assertEquals("Hello there! from Camel route", echo.echo("aaa"));
    }
}
