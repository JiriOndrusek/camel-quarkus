package org.apache.camel.quarkus.component.jt400.it;

import com.ibm.as400.access.AS400Message;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jt400.Jt400Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Jt400Routes extends RouteBuilder {

    @ConfigProperty(name = "cq.jt400.library")
    String jt400Library;

    @ConfigProperty(name = "cq.jt400.url")
    String jt400Url;

    @ConfigProperty(name = "cq.jt400.username")
    String jt400Username;

    @ConfigProperty(name = "cq.jt400.password")
    String jt400Password;

    @ConfigProperty(name = "cq.jt400.message-queue")
    String jt400MessageQueue;


    @ConfigProperty(name = "cq.jt400.message-replyto-queue")
    String jt400MessageReplyToQueue;

    @Override
    public void configure() throws Exception {
        from(getUrlForLibrary(jt400MessageQueue + "?sendingReply=true"))
                .id("inquiryRoute")
                .autoStartup(false)
                .choice()
                .when(header(Jt400Constants.MESSAGE_TYPE).isEqualTo(AS400Message.INQUIRY))
                .process((exchange) -> {
                    String reply = "reply to: " + exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody(reply);
                })
                .to(getUrlForLibrary(jt400MessageQueue));
    }

    private String getUrlForLibrary(String suffix) {
        return String.format("jt400://%s:%s@%s%s", jt400Username, jt400Password, jt400Url,
                "/QSYS.LIB/" + jt400Library + ".LIB/" + suffix);
    }
}
