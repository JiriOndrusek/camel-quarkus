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
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    public void sendSmtp() {
        //start route
        routeController("pop3ReceiveRoute", "start");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("subject", "Hello World")
                .queryParam("from", "camel@localhost")
                .queryParam("to", EMAIL_ADDRESS)
                .body("Hi how are you")
                .post("/mail/send")
                .then()
                .statusCode(204);

        // Need to receive using pop3 as there is no smtp consumer
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
        routeController("pop3ReceiveRoute", "stop");
    }

    //    @Test
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

    //    @Test
    //    public void testConsumer() {
    //        GreenMail greenMail = new GreenMail(); //uses test ports by default
    //        greenMail.start();
    //
    //        //start route
    //        routeController("receiveRoute", "start");
    //
    //        Config config = ConfigProvider.getConfig();
    //        Integer port = config.getValue("camel.mail.smtp.port", Integer.class);
    //        //send messages
    //        //        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "some subject", "some body",
    //        //                new ServerSetup(port, (String)null, "smtp"));
    //
    //        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost", "some subject",
    //                "some body");
    //
    //        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
    //
    //        //        RestAssured.given()
    //        //                .contentType(ContentType.JSON)
    //        //                .body(IntStream.range(1, 5).boxed().map(i -> "message " + i).collect(Collectors.toList()))
    //        //                .post("/mail/send")
    //        //                .then()
    //        //                .statusCode(204);
    //
    //        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
    //            //receive
    //            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceived/")
    //                    .then()
    //                    .statusCode(200)
    //                    .extract().as(List.class);
    //        }, list -> list.size() == 4
    //                && "message 1".equals(list.get(0).get("body"))
    //                && "message 2".equals(list.get(1).get("body"))
    //                && "message 3".equals(list.get(2).get("body"))
    //                && "message 4".equals(list.get(3).get("body")));
    //
    //        routeController("receiveRoute", "stop");
    //    }

    //    @Test
    public void testBatchConsumer() {
        //start route
        routeController("batchReceiveRoute", "start");
        //send messages
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(IntStream.range(1, 5).boxed().map(i -> "message " + i).collect(Collectors.toList()))
                .post("/mail/send")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 4

                && "message 1".equals(list.get(0).get("body"))
                && 0 == (Integer) list.get(0).get(ExchangePropertyKey.BATCH_INDEX.getName())
                && 3 == (Integer) list.get(0).get(ExchangePropertyKey.BATCH_SIZE.getName())
                && !((Boolean) list.get(0).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 2".equals(list.get(1).get("body"))
                && 1 == (Integer) list.get(1).get(ExchangePropertyKey.BATCH_INDEX.getName())
                && !((Boolean) list.get(1).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 3".equals(list.get(2).get("body"))
                && 2 == (Integer) list.get(2).get(ExchangePropertyKey.BATCH_INDEX.getName())
                && ((Boolean) list.get(2).get(ExchangePropertyKey.BATCH_COMPLETE.getName()))

                && "message 4".equals(list.get(3).get("body"))
                && 0 == (Integer) list.get(3).get(ExchangePropertyKey.BATCH_INDEX.getName()));

        routeController("batchReceiveRoute", "stop");
    }

    //    @Test
    public void testAttachment() throws Exception {
        //        routeController("attachmentRoute", "start");

        RestAssured.get("/mail/sendAttachment/logo.jpeg")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceived/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 1
                && list.get(0).size() == 2
                && "Sending logo.jpeg!".equals(list.get(0).get("body"))
                && ("image/jpeg; name=logo.jpeg".equals(list.get(0).get("logo.jpeg_contentType")) ||
                        "application/octet-stream; name=logo.jpeg".equals(list.get(0).get("logo.jpeg_contentType"))));

        //        routeController("attachmentRoute", "stop");
    }

    //    @Test
    public void testConverters() throws Exception {
        routeController("convertersRoute", "start");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(CollectionHelper.mapOf("Subject", "Camel rocks"))
                .post("/mail/sendWithHeaders/send/Hello World")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
            //receive
            return (List<Map<String, Object>>) RestAssured.get("/mail/getReceivedAsString/")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);
        }, list -> list.size() == 1
                && "Hello World".equals(list.get(0).get("body")));

        routeController("convertersRoute", "stop");
    }

    //    @Test
    public void testSort() {
        List<String> msgs = IntStream.range(1, 5).boxed().map(i -> ("message " + i)).collect(Collectors.toList());
        Collections.reverse(msgs);
        //send messages
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

        //        //wait for finish
        //        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
        //                () -> RestAssured
        //                        .get("/mail/route/" + routeId + "/status")
        //                        .then()
        //                        .statusCode(200)
        //                        .extract().asString(),
        //                Matchers.is("start".equals(operation) ? ServiceStatus.Started.name() : ServiceStatus.Stopped.name()));
    }

    //    @Test
    //    public void testSendAndReceiveMails() throws Exception {
    //
    //        String email = "USERNAME@gmail.com";
    //
    //        MockEndpoint resultEndpoint = getMockEndpoint("mock:in");
    //        resultEndpoint.expectedBodiesReceived("Test Email Body\r\n");
    //
    //        Map<String, Object> headers = new HashMap<>();
    //        headers.put("To", email);
    //        headers.put("From", email);
    //        headers.put("Reply-to", email);
    //        headers.put("Subject", "SSL/TLS Test");
    //
    //        template.sendBodyAndHeaders("direct:in", "Test Email Body", headers);
    //
    //        resultEndpoint.assertIsSatisfied();
    //    }

    //    @Test
    //    public void testEmail() throws InterruptedException, MessagingException {
    //        SimpleMailMessage message = new SimpleMailMessage();
    //        message.setFrom("test@sender.com");
    //        message.setTo("test@receiver.com");
    //        message.setSubject("test subject");
    //        message.setText("test message");
    //        //First we need to call the actual method of EmailSErvice
    //        emailSender.send(message);
    //        //Then after that using GreenMail need to verify mail sent or not
    //        assertTrue(testSmtp.waitForIncomingEmail(5000, 1));
    //        Message[] messages = testSmtp.getReceivedMessages();
    //        assertEquals(1, messages.length);
    //        assertEquals("test subject", messages[0].getSubject());
    //        String body = GreenMailUtil.getBody(messages[0]).replaceAll("=\r?\n", "");
    //        assertEquals("test message", body);
    //    }
}
