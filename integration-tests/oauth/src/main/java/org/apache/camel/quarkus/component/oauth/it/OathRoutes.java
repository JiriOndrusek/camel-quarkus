package org.apache.camel.quarkus.component.oauth.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.oauth.OAuthBearerTokenProcessor;
import org.apache.camel.oauth.OAuthClientCredentialsProcessor;
import org.apache.camel.oauth.OAuthCodeFlowCallback;

@ApplicationScoped
public class OathRoutes extends RouteBuilder {

    @Inject
    private CamelContext camelContext;

    @Override
    public void configure() throws Exception {
        from("platform-http:/plain")
                .routeId("plain")
                .setBody(simple("Hello ${header.name} - No auth"));

        from("platform-http:/credentials")
                .routeId("credentials")
                // Obtain an Authorization Token
                .process(new OAuthClientCredentialsProcessor())
                // Extract the Authorization Token
                .process(exc -> {
                    var msg = exc.getMessage();
                    var authToken = msg.getHeader("Authorization", String.class);
                    exc.getIn().setBody(authToken);
                });

        from("platform-http:/protected")
                .routeId("protected")
                // Obtain an Authorization Token
                .process(new OAuthCodeFlowCallback())
                // Extract the Authorization Token
                .process(exc -> {
                    var msg = exc.getMessage();
                    var authToken = msg.getHeader("Authorization", String.class);
                    //                    context.getGlobalOptions().put("Authorization", authToken);
                })
                .setBody(simple("${body} - OAuthClientCredentials"));

        from("platform-http:/bearer")
                .routeId("bearer")
                .process(e -> {
                    camelContext.getGlobalOptions().put("Authorization", e.getIn().getHeader("Authorization", String.class));
                })
                .process(new OAuthBearerTokenProcessor())
                .setBody(simple("Hello ${header.name} - bearerToken"));
    }
}
