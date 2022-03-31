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
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Session;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailComponent;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    @Inject
    @Named("mailReceivedMessages")
    List<Map<String, Object>> mailReceivedMessages;

    @Inject
    CamelContext camelContext;

    @Override
    public void configure() throws Exception {
        bindToRegistry("smtp", smtp());
        //        bindToRegistry("sslContextParameters", MailTestHelper.createSslContextParameters());

        from("direct:mailtext")
                .setHeader("Subject", constant("Hello World"))
                .setHeader("To", constant("james@localhost"))
                .setHeader("From", constant("claus@localhost"))
                .to("smtp://localhost:3025?initialDelay=100&delay=100");

        from("direct:mimeMultipartMarshal")
                .marshal().mimeMultipart();
        from("direct:mimeMultipartUnmarshalMarshal")
                .unmarshal().mimeMultipart()
                .marshal().mimeMultipart();

        from("pop3://localhost:3110?initialDelay=100&delay=100"
                //        from("pop3://localhost:{{camel.mail.pop3.port}}?initialDelay=100&delay=100"
                + "&delete=true").id("receiveRoute").autoStartup(false)
                        .log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                        .process(e -> mailReceivedMessages
                                .add(Collections.singletonMap("body", e.getIn().getBody(String.class))));
        //
        //        from("pop3://jones@localhost?password=secret&initialDelay=100&delay=100"
        //                + "&delete=true").id("receiveRoute").autoStartup(false)
        //                        .process(e -> mailReceivedMessages
        //                                .add(Collections.singletonMap("body", e.getIn().getBody(String.class))));

        //        from("pop3://jones@localhost?password=secret&initialDelay=100&delay=100"
        //                + "&delete=true&maxMessagesPerPoll=3").id("batchReceiveRoute")
        //                        .autoStartup(false)
        //                        .process(e -> mailReceivedMessages.add(
        //                                CollectionHelper.mapOf("body", e.getIn().getBody(String.class),
        //                                        ExchangePropertyKey.BATCH_INDEX.getName(),
        //                                        e.getProperty(ExchangePropertyKey.BATCH_INDEX),
        //                                        ExchangePropertyKey.BATCH_COMPLETE.getName(),
        //                                        e.getProperty(ExchangePropertyKey.BATCH_COMPLETE),
        //                                        ExchangePropertyKey.BATCH_SIZE.getName(),
        //                                        e.getProperty(ExchangePropertyKey.BATCH_SIZE))));
        //
        //        from("pop3://james@mymailserver.com?password=secret&initialDelay=100&delay=100")
        //                .id("attachmentRoute").autoStartup(false)
        //                .process(e -> {
        //                    Map<String, Object> values = new HashMap<>();
        //                    values.put("body", e.getIn().getBody(String.class));
        //                    for (Map.Entry<String, DataHandler> entry : e.getIn(AttachmentMessage.class).getAttachments().entrySet()) {
        //                        values.put(entry.getKey() + "_contentType", e.getIn(AttachmentMessage.class)
        //                                .getAttachmentObject(entry.getKey()).getDataHandler().getContentType());
        //                    }
        //                    mailReceivedMessages.add(values);
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

        //        @Produces
        //        Store prepareMailbox() throws Exception {
        //            // connect to mailbox
        //            Mailbox.clearAll();
        //            JavaMailSender sender = new DefaultJavaMailSender();
        //            Store store = sender.getSession().getStore("pop3");
        //            return store;
        //        }

        //        @Produces
        //        Store prepareGreenmail() throws Exception {
        //            // connect to mailbox
        //            GreenMail greenMail = new GreenMail(); //uses test ports by default
        //            greenMail.start();
        //        }

    }
}
