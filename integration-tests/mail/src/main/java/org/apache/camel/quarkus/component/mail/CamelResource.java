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
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sun.mail.imap.SortTerm;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.DefaultAttachment;
import org.apache.camel.component.mail.DefaultJavaMailSender;
import org.apache.camel.component.mail.JavaMailSender;
import org.apache.camel.component.mail.MailConverters;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.component.mail.MailSorter;

@Path("/mail")
@ApplicationScoped
public class CamelResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    //    @Inject
    Store store;

    @Inject
    CamelContext camelContext;

    @Inject
    @Named("mailReceivedMessages")
    List<Map<String, Object>> mailReceivedMessages;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendMail(
            @QueryParam("subject") String subject,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            String body) {

        producerTemplate.send("direct:sendMail" , exchange -> {
            org.apache.camel.Message message = exchange.getMessage();
            message.setHeader("Subject", subject);
            message.setHeader("From", from);
            message.setHeader("To", to);
            message.setBody(body);
        });
    }


    @Path("/send/attachment/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendMailWithAttachment(
            @PathParam("fileName") String fileName,
            @QueryParam("subject") String subject,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            String body) {

        producerTemplate.send("direct:sendMail", exchange -> {
            AttachmentMessage in = exchange.getMessage(AttachmentMessage.class);

            DefaultAttachment attachment = new DefaultAttachment(new FileDataSource(fileName));
            in.addAttachmentObject(fileName, attachment);

            org.apache.camel.Message message = exchange.getMessage();
            message.setHeader("Subject", subject);
            message.setHeader("From", from);
            message.setHeader("To", to);
            message.setBody(body);
        });
    }

    @Path("/mimeMultipartUnmarshalMarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipartUnmarshalMarshal(String body) {
        return producerTemplate.requestBody("direct:mimeMultipartUnmarshalMarshal", body, String.class);
    }

    @Path("/mimeMultipartMarshal/{fileName}/{fileContent}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipart(
            @PathParam("fileName") String fileName,
            @PathParam("fileContent") String fileContent,
            String body) {

        return producerTemplate.request("direct:mimeMultipartMarshal", e -> {
            AttachmentMessage in = e.getMessage(AttachmentMessage.class);
            in.setBody(body);
            in.setHeader(Exchange.CONTENT_TYPE, "text/plain;charset=iso8859-1;other-parameter=true");
            in.setHeader(Exchange.CONTENT_ENCODING, "UTF8");

            DefaultAttachment attachment = new DefaultAttachment(new ByteArrayDataSource(fileContent, "text/plain"));
            attachment.addHeader("Content-Description", "Sample Attachment Data");
            attachment.addHeader("X-AdditionalData", "additional data");
            in.addAttachmentObject(fileName, attachment);

        }).getMessage().getBody(String.class);
    }

    // ------------------------------------------------


    @Path("/getReceived")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getReceived() {
        return mailReceivedMessages;
    }

    @Path("/getReceivedAsString")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getReceivedAsString() throws MessagingException, IOException {
        List<Map<String, Object>> result = new LinkedList();
        for (Map<String, Object> email : mailReceivedMessages) {
            Message mm = ((MailMessage) email.get("body")).getMessage();
            InputStream is = MailConverters.toInputStream(mm);
            result.add(Collections.singletonMap("body", (Object) camelContext.getTypeConverter().convertTo(String.class, is)));
        }
        mailReceivedMessages.clear();
        return result;

    }

    @Path("/clear")
    @GET
    public void clear() {
        mailReceivedMessages.clear();
    }

    @GET
    @Path("/route/{routeId}/{operation}")
    @Produces(MediaType.TEXT_PLAIN)
    public String controlRoute(@PathParam("routeId") String routeId, @PathParam("operation") String operation)
            throws Exception {
        switch (operation) {
        case "stop":
            camelContext.getRouteController().stopRoute(routeId);
            break;
        case "start":
            camelContext.getRouteController().startRoute(routeId);
            break;
        case "status":
            return camelContext.getRouteController().getRouteStatus(routeId).name();

        }
        return null;
    }

    @GET
    @Path("/sendAttachment/{filename}")
    public void sendAttachment(@PathParam("filename") String filename) throws Exception {
        Endpoint endpoint = camelContext.getEndpoint("smtp://localhost:3025?to=info@mycompany.com");

        // create the exchange with the mail message that is multipart with a file and a Hello World text/plain message.
        Exchange exchange = endpoint.createExchange();
        AttachmentMessage in = exchange.getIn(AttachmentMessage.class);
        in.setBody("Sending " + filename + "!");
        DefaultAttachment att = new DefaultAttachment(
                new ByteArrayDataSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/" + filename),
                        "image/jpeg"));
        att.addHeader("Content-Description", "some sample content");
        in.addAttachmentObject(filename, att);

        // create a producer that can produce the exchange (= send the mail)
        Producer producer = endpoint.createProducer();
        // start the producer
        producer.start();
        // and let it go (processes the exchange by sending the email)
        producer.process(exchange);

        producer.stop();
    }

    @Path("/sort")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> sort(List<String> messages) throws Exception {
        JavaMailSender sender = new DefaultJavaMailSender();
        // inserts new messages
        Message[] msgs = new Message[messages.size()];
        int i = 0;
        for (String msg : messages) {
            msgs[i] = new MimeMessage(sender.getSession());
            msgs[i].setHeader("Subject", msg);
            msgs[i++].setText(msg);
        }
        MailSorter.sortMessages(msgs, new SortTerm[] {
                SortTerm.SUBJECT });

        return Stream.of(msgs).map(m -> {
            try {
                return String.valueOf(m.getContent());
            } catch (Exception e) {
                return "error";
            }
        }).collect(Collectors.toList());
    }

}
