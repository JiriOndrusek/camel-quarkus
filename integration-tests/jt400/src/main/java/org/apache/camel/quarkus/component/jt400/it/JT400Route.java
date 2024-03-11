package org.apache.camel.quarkus.component.jt400.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JT400Route extends RouteBuilder {

    @ConfigProperty(name = "cq.jt400.url")
    String jt400Url;

    @ConfigProperty(name = "cq.jt400.username")
    String jt400USername;

    @ConfigProperty(name = "cq.jt400.password")
    String jt400Password;

    @Override
    public void configure() {
        from("direct:executeProgram")
                .process(exchange -> {
                    String usrSpc = "MYUSRSPACEQGPL      ";
                    Object[] parms = new Object[] {
                            usrSpc, // Qualified user space name
                            1, // starting position
                            16, // length of data
                            "" // output
                    };
                    exchange.getIn().setBody(parms);
                })
                .to(getUrl("/qsys.lib/QUSRTVUS.PGM?fieldsLength=20,4,4,16&outputFieldsIdx=3"))
                .setBody(simple("${body}"))
                .to("direct:foo");
    }

    private String getUrl(String suffix) {
        return String.format("jt400://%s:%s@%s%s", jt400USername, jt400Password, jt400Url, suffix);
    }
}
