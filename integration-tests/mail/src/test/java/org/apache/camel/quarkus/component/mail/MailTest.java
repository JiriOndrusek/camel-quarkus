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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.ServiceStatus;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.apache.camel.quarkus.component.mail.CamelRoute.EMAIL_ADDRESS;
import static org.apache.camel.quarkus.component.mail.CamelRoute.PASSWORD;
import static org.apache.camel.quarkus.component.mail.CamelRoute.USERNAME;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(MailTestResource.class)
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

    @BeforeEach
    public void beforeEach() {
        // Configure users
        Config config = ConfigProvider.getConfig();
        String userJson = String.format("{ \"email\": \"%s\", \"login\": \"%s\", \"password\": \"%s\"}", EMAIL_ADDRESS,
                USERNAME, PASSWORD);
        RestAssured.given()
                .port(config.getValue("mail.api.port", Integer.class))
                .contentType(ContentType.JSON)
                .body(userJson)
                .post("/api/user")
                .then()
                .statusCode(200);

        String users = RestAssured.get("http://localhost:" + config.getValue("mail.api.port", Integer.class) + "/api/user")
                .then()
                .statusCode(200)
                .extract().asString();

        System.out.println("**************************** Users: " + users);
    }

    @AfterEach
    public void afterEach() {
        // Clear mailboxes
        Config config = ConfigProvider.getConfig();
        RestAssured.given()
                .port(config.getValue("mail.api.port", Integer.class))
                .post("/api/service/reset")
                .then()
                .statusCode(200)
                .body("message", is("Performed reset"));

        RestAssured.get("/mail/clear")
                .then()
                .statusCode(204);
    }

    //    @ParameterizedTest
    @ValueSource(strings = { "pop3", "imap" })
    public void receive(String protocol) {
        //start route
        routeController(protocol + "ReceiveRoute", "start");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("subject", "Hello World")
                .queryParam("from", "camel@localhost")
                .queryParam("to", EMAIL_ADDRESS)
                .body("Hi how are you")
                .post("/mail/send")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 1
                && "Hi how are you".equals(list.get(0).get("content"))
                && "Hello World".equals(list.get(0).get("subject")));
        //stop route
        routeController(protocol + "ReceiveRoute", "stop");
    }

    //    @Test
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
                .post("/mail/mimeMultipartUnmarshalMarshal")
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

    //    @Test
    public void testAttachments() throws IOException, URISyntaxException {
        routeController("pop3ReceiveRoute", "start");

        String mailBodyContent = "Test mail content";
        String attachmentContent = "Attachment " + mailBodyContent;
        java.nio.file.Path attachmentPath = Files.createTempFile("cq-attachment", ".txt");
        Files.write(attachmentPath, attachmentContent.getBytes(StandardCharsets.UTF_8));

        try {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("subject", "Test attachment message")
                    .queryParam("from", "camel@localhost")
                    .queryParam("to", EMAIL_ADDRESS)
                    .body(mailBodyContent)
                    .post("/mail/send/attachment/{fileName}", attachmentPath.toAbsolutePath().toString())
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("subject", "Test attachment message")
                    .queryParam("from", "camel@localhost")
                    .queryParam("to", EMAIL_ADDRESS)
                    .body(mailBodyContent)
                    .post("/mail/send/attachment/{fileName}", Path.of(Thread.currentThread().getContextClassLoader()
                            .getResource("data/logo.jpeg").toURI()).toAbsolutePath().toString())
                    .then()
                    .statusCode(204);

            Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath(),
                    path -> 2 == (Integer) path.get("size")

                            && "Test mail content".equals(path.get("[0].content"))
                            && "Test attachment message".equals(path.get("[0].subject"))
                            && attachmentPath.getFileName().toString().equals(path.get("[0].attachments[0].attachmentFilename"))
                            && attachmentContent.equals(path.get("[0].attachments[0].attachmentContent"))

                            && "Test mail content".equals(path.get("[1].content"))
                            && "Test attachment message".equals(path.get("[1].subject"))
                            && "logo.jpeg".equals(path.get("[1].attachments[0].attachmentFilename"))
                            && (path.get("[1].attachments[0].attachmentContentType").toString().startsWith("image/jpeg")
                                    || path.get("[1].attachments[0].attachmentContentType").toString()
                                            .startsWith("application/octet-stream")));
        } finally {
            Files.deleteIfExists(attachmentPath);
        }

        routeController("pop3ReceiveRoute", "stop");
    }

    //    @Test
    public void testBatchConsumer() {
        //start route
        routeController("batchReceiveRoute", "start");
        //send messages
        IntStream.range(1, 5).boxed().forEach(i -> RestAssured.given()
                .contentType(ContentType.JSON)
                .contentType(ContentType.TEXT)
                .queryParam("subject", "Test batch consumer")
                .queryParam("from", "camel@localhost")
                .queryParam("to", EMAIL_ADDRESS)
                .body("message " + i)
                .post("/mail/send")
                .then()
                .statusCode(204));

        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 4

                && "message 1".equals(list.get(0).get("content"))
                && "Test batch consumer".equals(list.get(0).get("subject"))
                && "0".equals(list.get(0).get(ExchangePropertyKey.BATCH_INDEX.getName()).toString())
                && "3".equals(list.get(0).get(ExchangePropertyKey.BATCH_SIZE.getName()).toString())
                && !((Boolean) list.get(0).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 2".equals(list.get(1).get("content"))
                && "Test batch consumer".equals(list.get(1).get("subject"))
                && "1".equals(list.get(1).get(ExchangePropertyKey.BATCH_INDEX.getName()).toString())
                && !((Boolean) list.get(1).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 3".equals(list.get(2).get("content"))
                && "Test batch consumer".equals(list.get(2).get("subject"))
                && "2".equals(list.get(2).get(ExchangePropertyKey.BATCH_INDEX.getName()).toString())
                && ((Boolean) list.get(2).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 4".equals(list.get(3).get("content"))
                && "Test batch consumer".equals(list.get(3).get("subject"))
                && "0".equals(list.get(3).get(ExchangePropertyKey.BATCH_INDEX.getName()).toString()));

        routeController("batchReceiveRoute", "stop");
    }

    @Test
    public void testConverters() throws Exception {
        routeController("convertersRoute", "start");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .contentType(ContentType.TEXT)
                .queryParam("subject", "Camel Rocks")
                .queryParam("from", "camel@localhost")
                .queryParam("to", EMAIL_ADDRESS)
                .body("Hello World ")
                .post("/mail/send")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceivedAsString/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 1
                && ((String) list.get(0).get("body")).matches("Hello World\\s*"));

        routeController("convertersRoute", "stop");
    }

    @Test
    public void testSort() {
        List<String> msgs = IntStream.range(1, 5).boxed().map(i -> ("message " + i)).collect(Collectors.toList());
        //messages will be sent in reverse order
        Collections.reverse(msgs);

        List<String> sorted = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(msgs)
                .post("/mail/sort")
                .then()
                .statusCode(200)
                .extract().as(List.class);

        Assertions.assertEquals(4, sorted.size());
        Assertions.assertTrue(sorted.get(0).contains("message 1"));
        Assertions.assertTrue(sorted.get(1).contains("message 2"));
        Assertions.assertTrue(sorted.get(2).contains("message 3"));
        Assertions.assertTrue(sorted.get(3).contains("message 4"));
    }

    // helper methods

    private void routeController(String routeId, String operation) {
        RestAssured.given()
                .get("/mail/route/" + routeId + "/" + operation)
                .then().statusCode(204);

        //wait for finish
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
                () -> {
                    String status = RestAssured
                            .get("/mail/route/" + routeId + "/status")
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    return status
                            .equals("start".equals(operation) ? ServiceStatus.Started.name() : ServiceStatus.Stopped.name());
                });
    }
}
