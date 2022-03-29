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
package org.apache.camel.quarkus.component.mail;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.ServiceStatus;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
public class MailTest {
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\r\n[^\r\n]+");
    private static final String EXPECTED_TEMPLATE = "${delimiter}\r\n"
            + "Content-Type: text/plain; charset=UTF8; other-parameter=true\r\n"
            + "Content-Transfer-Encoding: 8bit\r\n"
            + "\r\n"
            + "Hello multipart!"
            + "${delimiter}\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Transfer-Encoding: 8bit\r\n"
            + "Content-Disposition: attachment; filename=file.txt\r\n"
            + "Content-Description: Sample Attachment Data\r\n"
            + "X-AdditionalData: additional data\r\n"
            + "\r\n"
            + "Hello attachment!"
            + "${delimiter}--\r\n";

    @Test
    public void testSendAsMail() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hi how are you")
                .post("/mail/route/mailtext")
                .then()
                .statusCode(200);

        RestAssured.given()
                .get("/mock/{username}/size", "james@localhost")
                .then()
                .body(is("1"));
        RestAssured.given()
                .get("/mock/{username}/{id}/content", "james@localhost", 0)
                .then()
                .body(is("Hi how are you"));
        RestAssured.given()
                .get("/mock/{username}/{id}/subject", "james@localhost", 0)
                .then()
                .body(is("Hello World"));
    }

    @Test
    public void mimeMultipartDataFormat() {
        final String actual = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello multipart!")
                .post("/mail/mimeMultipartMarshal/file.txt/Hello attachment!")
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertMultipart(EXPECTED_TEMPLATE, actual);

        final String unmarshalMarshal = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(actual)
                .post("/mail/route/mimeMultipartUnmarshalMarshal")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertMultipart(EXPECTED_TEMPLATE, unmarshalMarshal);

    }

    private void assertMultipart(final String expectedPattern, final String actual) {
        final Matcher m = DELIMITER_PATTERN.matcher(actual);
        if (!m.find()) {
            Assertions.fail("Mime delimiter not found in body: " + actual);
        }
        final String delim = m.group();
        final String expected = expectedPattern.replace("${delimiter}", delim);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testConsumer() {

        //start route
        routeController("start", null);
        //wait for start
        routeController("status", ServiceStatus.Started.name());
        //send messages
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(IntStream.range(1, 5).boxed().map(i -> "message " + i).collect(Collectors.toList()))
                .post("/mail/send")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            //receive
            return RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 4
                && "message 1".equals(list.get(0))
                && "message 2".equals(list.get(1))
                && "message 3".equals(list.get(2))
                && "message 4".equals(list.get(3)));
    }

    private String routeController(String operation, String expectedResult) {
        String routeId = "receiveRoute";
        if (expectedResult == null) {
            RestAssured.given()
                    .get("/mail/route/" + routeId + "/" + operation)
                    .then().statusCode(204);
        } else {
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
                    () -> RestAssured
                            .get("/mail/route/" + routeId + "/" + operation)
                            .then()
                            .statusCode(200)
                            .extract().asString(),
                    Matchers.is(expectedResult));
        }

        return null;
    }
}
