package org.apachencamel.quarkus.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

// replaces CreateCamelContextPerTestTrueTest
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestProfile(CallbacksPerTestTrueTest.class)
public class CallbacksPerTestTrueTest extends AbstractCallbacksTest {

    public CallbacksPerTestTrueTest() {
        super(CallbacksPerTestTrueTest.class.getSimpleName());
    }

    @AfterAll
    public static void shouldTearDown() {
        testAfterAll(CallbacksPerTestTrueTest.class.getSimpleName(), (callback, count) -> {
            switch (callback) {
            case doSetup:
                assertCount(1, count, callback);
                break;
            case contextCreation:
                assertCount(1, count, callback);
                break;
            case postSetup:
                assertCount(1, count, callback);
                break;
            case postTearDown:
                assertCount(1, count, callback);
                break;
            case preSetup:
                assertCount(1, count, callback);
                break;
            default:
                throw new IllegalArgumentException();
            }
        });
    }
}
