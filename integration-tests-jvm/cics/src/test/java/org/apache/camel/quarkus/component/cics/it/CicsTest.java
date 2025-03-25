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
import java.util.stream.Stream;

import com.redhat.camel.component.cics.CICSConstants;
import com.redhat.camel.component.cics.support.CICSDataExchangeType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.redhat.camel.component.cics.CICSConstants.*;

//@TestCertificates(certificates = {
//        @Certificate(name = "ctg-server", formats = { Format.PKCS12,
//                Format.PEM }, password = "changeit") })
//@EnabledIfEnvironmentVariable(named = "CTG_CLIENT_VERSION", matches = ".+")
@QuarkusTestResource(CicsTestResource.class)
@QuarkusTest
class CicsTest {

    private final Map<String, Object> containers = Map.of("FirstChar", "My First Container",
            "SecondByteArray", "My Second Container".getBytes());

    static Stream<Arguments> typeMatrix() {
        //        String[] gatewayFactories = { "noFactory", "factory", "pooledFactory" };
        String[] gatewayFactories = { "noFactory" };
        // channel requires a special app deployed in thw container image, which is not present
        // only commarea has relevant app deployed in the docker image
        String[] dataExchangeTypes = { CICSDataExchangeType.COMMAREA.name() };

        return Stream.of(dataExchangeTypes)
                .flatMap(type -> Stream.of(gatewayFactories)
                        .map(factory -> Arguments.of(type, factory)));
    }

    @MethodSource("typeMatrix")
    @ParameterizedTest
    public void pooledFactory(String dataExchangeType, String factory) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("channel".equals(dataExchangeType) ? containers : "")
                .post("/cics/eciReady/" + dataExchangeType + "/" + factory)
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

}
