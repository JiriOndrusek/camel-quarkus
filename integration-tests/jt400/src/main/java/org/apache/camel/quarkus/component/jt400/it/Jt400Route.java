package org.apache.camel.quarkus.component.jt400.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class Jt400Route extends RouteBuilder {

    @Inject
    @Named("collected-data")
    Map<String, List<String>> collectedNames;

    @Override
    public void configure() {
        //        from("jt400://username:password@system/lib.lib/MSGOUTDQ.DTAQ?keyed=true&searchKey=MYKEY&searchType=GE&guiAvailable=true")
        //                .autoStartup(false)
        //                .log("${body}");

        //        from("jt400://username:password@system/qsys.lib/MSGOUTDQ.DTAQ?connectionPool=#mockPool&keyed=true&format=binary&searchKey=MYKEY&searchType=GE")
        //                .process(
        //                        e -> collectedNames.get("queue").add(e.getMessage().getBody(String.class)));
    }

}
