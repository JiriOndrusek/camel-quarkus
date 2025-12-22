package org.apache.camel.quarkus.component.cyberark.vault.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CyberArkRoutes extends RouteBuilder {

    @ConfigProperty(name = "conjur.url")
    String url;
    @ConfigProperty(name = "conjur.account")
    String account;
    @ConfigProperty(name = "conjur.write.username")
    String writeUsername;
    @ConfigProperty(name = "conjur.write.apiKey")
    String writeApiKey;
    @ConfigProperty(name = "conjur.read.username")
    String readUsername;
    @ConfigProperty(name = "conjur.read.apiKey")
    String readApiKey;

    @Override
    public void configure() throws Exception {

        from("direct:createSecret")
                .toF("cyberark-vault:secret?operation=createSecret&secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, writeUsername, writeApiKey)
                .log("Secret created/updated");

        from("direct:createSecretUnauthorized")
                .log("************************************************ unauthorized ****************************")
                .toF("cyberark-vault:secret?operation=createSecret&secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, readUsername, readApiKey)
                .log("Secret created/updated");

        from("direct:getSecret")
                .toF("cyberark-vault:secret?secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, readUsername, readApiKey)
                .log("Retrieved secret: ${body}");

    }
}
