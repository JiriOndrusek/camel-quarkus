package org.apache.camel.quarkus.component.jt400.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class Jt400Route extends RouteBuilder {

    @Override
    public void configure() {
        //        from("jt400://username:password@system/lib.lib/MSGOUTDQ.DTAQ?keyed=true&searchKey=MYKEY&searchType=GE&guiAvailable=true")
        //                .autoStartup(false)
        //                .log("${body}");

        from("jt400://username:password@system/qsys.lib/MSGOUTDQ.DTAQ?connectionPool=#mockPool&keyed=true&searchKey=MYKEY&searchType=GE")
                .log("${body}");
    }

}
