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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.activation.DataHandler;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailComponent;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    @Inject
    @Named("mailReceivedMessages")
    List<Map<String, Object>> mailReceivedMessages;

    @Inject
    CamelContext camelContext;

    static final String EMAIL_ADDRESS = "test@localhost";
    static final String USERNAME = "test";
    static final String PASSWORD = "s3cr3t";

    @ConfigProperty(name = "mail.smtp.port")
    int smtpPort;

    @ConfigProperty(name = "mail.pop3.port")
    int pop3Port;

    @ConfigProperty(name = "mail.imap.port")
    int imapPort;

    @Override
    public void configure() {
        from("direct:sendMail")
                .toF("smtp://localhost:%d?username=%s&password=%s", smtpPort, USERNAME, PASSWORD);

        from("direct:mimeMultipartMarshal")
                .marshal().mimeMultipart();

        from("direct:mimeMultipartUnmarshalMarshal")
                .unmarshal().mimeMultipart()
                .marshal().mimeMultipart();

        fromF("pop3://localhost:%d?initialDelay=100&delay=500&username=%s&password=%s&delete=true", pop3Port, USERNAME, PASSWORD)
                .id("pop3ReceiveRoute")
                .autoStartup(false)
                .process(exchange -> handleMail(exchange));

        fromF("imap://localhost:%d?initialDelay=100&delay=500&username=%s&password=%s&delete=true", imapPort, USERNAME, PASSWORD)
                .id("imapReceiveRoute")
                .autoStartup(false)
                .process(exchange -> handleMail(exchange));

//        fromF("pop3://localhost:%d?initialDelay=100&delay=500&username=%s&password=%s"
//                + "&delete=true&maxMessagesPerPoll=3", pop3Port, USERNAME, PASSWORD)
//                .id("batchReceiveRoute")
//                .autoStartup(false)
//                .process(e -> {
//                    Map<String, Object> map = handleMail(e);
//                    map.put(ExchangePropertyKey.BATCH_INDEX.getName(), e.getProperty(ExchangePropertyKey.BATCH_INDEX));
//                    map.put(ExchangePropertyKey.BATCH_COMPLETE.getName(), e.getProperty(ExchangePropertyKey.BATCH_COMPLETE));
//                    map.put(ExchangePropertyKey.BATCH_SIZE.getName(), e.getProperty(ExchangePropertyKey.BATCH_SIZE));
//                });

//        from("pop3://james@mymailserver.com?password=secret&initialDelay=100&delay=100")
//                .id("attachmentRoute").autoStartup(false)
//                .process(e -> {
//                    Map<String, Object> map = handleMail(e);
//                    for (Map.Entry<String, DataHandler> entry : e.getIn(AttachmentMessage.class).getAttachments().entrySet()) {
//                        map.put(entry.getKey() + "_contentType", e.getIn(AttachmentMessage.class)
//                                .getAttachmentObject(entry.getKey()).getDataHandler().getContentType());
//                    }
//                });
        //
        //        from("direct:send").to("smtp://localhost?username=james@localhost");
        //
        //        from("pop3://localhost?username=james&password=secret&initialDelay=100&delay=100").id("convertersRoute")
        //                .autoStartup(false)
        //                .process(e -> mailReceivedMessages.add(
        //                        CollectionHelper.mapOf("body", e.getIn().getBody(MailMessage.class))));
        //
        //        String username = "USERNAME@gmail.com";
        //        String imapHost = "imap.gmail.com";
        //        String smtpHost = "smtp.gmail.com";
        //        String password = "PASSWORD";
        //
        //
        //        from("imaps://" + imapHost + "?username=" + username + "&password=" + password
        //                + "&delete=false&unseen=true&fetchSize=1&useFixedDelay=true&initialDelay=100&delay=100").to("mock:in");
        //
        //        from("direct:ssh").to("smtps://" + smtpHost + "?username=" + username + "&password=" + password);
    }


    private  Map<String, Object> handleMail(Exchange exchange) throws MessagingException {
        Map<String, Object> result = new HashMap<>();
        MailMessage mailMessage = exchange.getMessage(MailMessage.class);
        AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
        Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
        if (attachments != null) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            attachments.forEach((id, dataHandler) -> {
                JsonObjectBuilder attachmentObject = Json.createObjectBuilder();
                attachmentObject.add("attachmentFilename", dataHandler.getName());
                attachmentObject.add("attachmentContentType", dataHandler.getContentType());

                if(dataHandler.getContentType().startsWith("text/plain")) {
                    try {
                        String content = camelContext.getTypeConverter().convertTo(String.class,
                                dataHandler.getInputStream());
                        attachmentObject.add("attachmentContent", content);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }



                arrayBuilder.add(attachmentObject.build());
            });

            result.put("attachments", arrayBuilder.build());
        }

//      todo  Folder folder = mailMessage.getOriginalMessage().getFolder();
//        if (!folder.isOpen()) {
//            folder.open(Folder.READ_ONLY);
//        }

        result.put("subject", mailMessage.getMessage().getSubject());
        result.put("content", mailMessage.getBody(String.class).trim());

        mailReceivedMessages.add(result);

        return result;
    }

    MailComponent smtp() {
        MailComponent mail = new MailComponent(camelContext);
        Session session = Session.getInstance(new Properties());
        mail.getConfiguration().setSession(session);
        return mail;
    }

    static class Producers {

        @Singleton
        @Produces
        @Named("mailReceivedMessages")
        List<Map<String, Object>> mailReceivedMessages() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
