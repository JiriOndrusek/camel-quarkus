package org.apache.camel.quarkus.test.support.aws2;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public abstract class BaseAWs2TestSupport {

    //todo try to register a second rest api on /aws and do not require this parametet
    private final String restPath;

    private boolean clearDefaultCredentialsProvider;

    public BaseAWs2TestSupport(String restPath) {
        this.restPath = restPath;
    }

    /**
     * Testing method used for {@link #useDefaultCredentialsProviderTest()}.
     *
     * This method is called twice.
     * 1 - Credentials are not set, therefore this method should fail.
     * 2 - Credentials are set, there this method should succeed.
     *
     * Returns true if test passes, fa;se otherwise.
     */
    public abstract void testMethodForDefaultCredentialsProvider();

    //test can be executed only if mock backend is used and no defaultCredentialsprovider is defined in the system
    @ExtendWith(Aws2DefaultCredentialsProviderAvailabilityCondition.class)
    @Test
    public void successfulDefaultCredentialsProviderTest() {
        RestAssured.given()
                .body(true)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);

        RestAssured.given()
                .body(false)
                .post(restPath + "/initializeDefaultCredentials")
                .then()
                .statusCode(200);

        //should succeed
        testMethodForDefaultCredentialsProvider();

        RestAssured.given()
                .body(false)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);
    }

    //test can be executed only if mock backend is used and no defaultCredentialsprovider is defined in the system
    @ExtendWith(Aws2DefaultCredentialsProviderAvailabilityCondition.class)
    @Test
    public void failingDefaultCredentialsProviderTest() {
        RestAssured.given()
                .body(true)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);

        // should fail without credentials for aws
        testMethodForDefaultCredentialsProvider();

        RestAssured.given()
                .body(false)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);
    }

    public boolean isClearDefaultCredentialsProvider() {
        return clearDefaultCredentialsProvider;
    }

    public void setClearDefaultCredentialsProvider(boolean clearDefaultCredentialsProvider) {
        this.clearDefaultCredentialsProvider = clearDefaultCredentialsProvider;
    }
}
