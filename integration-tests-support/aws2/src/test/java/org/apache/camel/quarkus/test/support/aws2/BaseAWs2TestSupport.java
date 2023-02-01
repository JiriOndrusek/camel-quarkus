package org.apache.camel.quarkus.test.support.aws2;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public abstract class BaseAWs2TestSupport implements DefaultCredentialsProviderAware, QuarkusTestAfterEachCallback {

    //todo try to register a second rest api on /aws and do not require this parametet
    private final String restPath;

    private boolean clearAwsSystemCredentials;

    public BaseAWs2TestSupport(String restPath) {
        this.restPath = restPath;
    }

    /**
     * Testing method used for {@link #successfulDefaultCredentialsProviderTest()} and
     * {@link #failingDefaultCredentialsProviderTest()}.
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
                .body(true)
                .post(restPath + "/initializeDefaultCredentials")
                .then()
                .statusCode(200);

        //should succeed
        testMethodForDefaultCredentialsProvider();

        if (isClearAwsSystemCredentials()) {
            RestAssured.given()
                    .body(false)
                    .post(restPath + "/initializeDefaultCredentials")
                    .then()
                    .statusCode(200);
        }

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
        Assertions.assertThrows(AssertionError.class, () -> testMethodForDefaultCredentialsProvider());

        RestAssured.given()
                .body(false)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {

    }

    public boolean isClearAwsSystemCredentials() {
        return clearAwsSystemCredentials;
    }

    public void setClearAwsSystemCredentials(boolean clearAwsSystemCredentials) {
        this.clearAwsSystemCredentials = clearAwsSystemCredentials;
    }
}
