package org.apache.camel.quarkus.component.azure.key.vault.it;

import io.restassured.RestAssured;

public class AzureKeyVaultUtil {

    static void deleteSecretImmediately(String secretName) {
        //we need to se identity by default, as the non-identity routes may not start
        AzureKeyVaultUtil.deleteSecretImmediately(secretName, true);
    }

    static void deleteSecretImmediately(String secretName, boolean useIdentity) {
        // Delete secret
        RestAssured.given()
                .delete("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                .then()
                .statusCode(200);

        // Purge secret
        RestAssured.given()
                .delete("/azure-key-vault/secret/" + useIdentity + "/{secretName}/purge", secretName)
                .then()
                .statusCode(200);

        // Confirm deletion
        RestAssured.given()
                .queryParam("identity", useIdentity)
                .get("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                .then()
                .statusCode(500);
    }
}
