package org.apachencamel.quarkus.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

// replaces CreateCamelContextPerTestTrueTest
//tdoo add check of all callbacks and context creation + the same check with perClass
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestProfile(CallbacksPerTestFalseTest.class)
public class CallbacksPerTestFalseTest extends AbstractCallbacksTest {

    public CallbacksPerTestFalseTest() {
        super(CallbacksPerTestFalseTest.class.getSimpleName());
    }

    @AfterAll
    public static void shouldTearDown() {
        testAfterAll(CallbacksPerTestFalseTest.class.getSimpleName(), (callback, count) -> {
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
