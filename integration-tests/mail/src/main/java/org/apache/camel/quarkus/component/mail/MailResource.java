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
import java.util.Map;

import javax.activation.FileDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.DefaultAttachment;
import org.apache.camel.component.mail.DefaultJavaMailSender;
import org.apache.camel.component.mail.JavaMailSender;

@Path("/mail")
@ApplicationScoped
public class MailResource {

    @Inject
    ProducerTemplate template;

    @Inject
    Store store;

    @Inject
    CamelContext camelContext;

    @Inject
    @Named("mailReceivedMessages")
    List<Map<String, Object>> mailReceivedMessages;

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String route(String statement, @PathParam("route") String route) throws Exception {
        return template.requestBody("direct:" + route, statement, String.class);
    }

    @Path("/mimeMultipartMarshal/{fileName}/{fileContent}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipart(String body, @PathParam("fileName") String fileName,
            @PathParam("fileContent") String fileContent) throws Exception {

        return template.request("direct:mimeMultipartMarshal", e -> {
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

    @Path("/send")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void send(List<String> messages) throws Exception {
        JavaMailSender sender = new DefaultJavaMailSender();
        store.connect("localhost", 25, "jones", "secret");
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        folder.expunge();

        // inserts new messages
        Message[] msgs = new Message[messages.size()];
        int i = 0;
        for (String msg : messages) {
            msgs[i] = new MimeMessage(sender.getSession());
            msgs[i].setHeader("Message-ID", "" + i);
            msgs[i++].setText(msg);
        }
        folder.appendMessages(msgs);
        folder.close(true);
        store.close();
    }

    @Path("/getReceived")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getReceived() {
        return mailReceivedMessages;
    }

    @Path("/clearReceived")
    @GET
    public void clearReceived() {
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
        Endpoint endpoint = camelContext.getEndpoint("smtp://james@mymailserver.com?password=secret");

        // create the exchange with the mail message that is multipart with a file and a Hello World text/plain message.
        Exchange exchange = endpoint.createExchange();
        AttachmentMessage in = exchange.getIn(AttachmentMessage.class);
        in.setBody("Sending " + filename + "!");
        DefaultAttachment att = new DefaultAttachment(
                new FileDataSource(Thread.currentThread().getContextClassLoader().getResource("data/" + filename).getFile()));
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

}
