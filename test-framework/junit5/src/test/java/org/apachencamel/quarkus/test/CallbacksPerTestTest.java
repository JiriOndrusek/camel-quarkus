package org.apachencamel.quarkus.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.test.junit.TestProfile;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.util.StopWatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// replaces CreateCamelContextPerTestTrueTest
//tdoo add check of all callbacks and context creation + the same check with perClass
@CamelQuarkusTest
@TestProfile(CallbacksPerTestTest.class)
public class CallbacksPerTestTest extends CamelQuarkusTestSupport {

    public enum Callback {
        postTearDown,
        doSetup,
        preSetup,
        postSetup;
    }

    public static void assertTest(Class testClass, Map<Callback, Long> counts) {
        for (Callback c : Callback.values()) {
            Assertions.assertEquals(1, counts.get(c), "Should only call " + c + " once.");
        }
    }

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Override
    protected void doPreSetup() throws Exception {
        createTmpFile(Callback.preSetup);
        super.doPostSetup();
    }

    @Override
    protected void doSetUp() throws Exception {
        createTmpFile(Callback.doSetup);
        super.doSetUp();
    }

    @Override
    protected void doPostSetup() throws Exception {
        createTmpFile(Callback.postSetup);
        super.doPostSetup();
    }

    @Override
    protected void doPostTearDown() throws Exception {
        createTmpFile(Callback.postTearDown);
        super.doPostTearDown();
    }

    @Test
    public void testMock() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Hello World");
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();
    }

    @AfterAll
    public static void shouldTearDown() {
        // we are called before doPostTearDown so lets wait for that to be
        // called
        Runnable r = () -> {
            Map<Callback, Long> counts = new HashMap<>();
            try {
                StopWatch watch = new StopWatch();
                while (watch.taken() < 5000) {
                    //LOG.debug("Checking for tear down called correctly");
                    try {
                        for (Callback c : Callback.values()) {
                            long count = doesTmpFileExist(c);
                            if (count > 0) {
                                counts.put(c, count);
                            }
                        }
                    } catch (Exception e) {
                        //ignore
                    }

                    if (counts.size() == Callback.values().length) {
                        break;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } finally {
                //  LOG.info("Should only call postTearDown 1 time per test class, called: " + POST_TEAR_DOWN.get());
                assertTest(null, counts);
            }
        };
        Thread t = new Thread(r);
        t.setDaemon(false);
        t.setName("shouldTearDown checker");
        t.start();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("mock:result");
            }
        };
    }

    private static void createTmpFile(Callback callback) throws Exception {
        Set<File> testDirs = Arrays.stream(Paths.get("target").toFile().listFiles())
                .filter(f -> f.isDirectory() && f.getName().startsWith(CallbacksPerTestTest.class.getSimpleName()))
                .collect(Collectors.toSet()); //todo only if there is onw

        Path tmpDir;
        if (testDirs.size() == 1) {
            tmpDir = testDirs.stream().findFirst().get().toPath();
        } else if (testDirs.size() > 1) {
            throw new RuntimeException();
        } else {
            tmpDir = Files.createTempDirectory(Paths.get("target"), CallbacksPerTestTest.class.getSimpleName());
            tmpDir.toFile().deleteOnExit();
        }

        Path tmpFile = Files.createTempFile(tmpDir, callback.name(), ".log");
        tmpFile.toFile().deleteOnExit();
    }

    private static long doesTmpFileExist(Callback callback) throws Exception {
        //find test dir
        Set<File> testDirs = Arrays.stream(Paths.get("target").toFile().listFiles())
                .filter(f -> f.isDirectory() && f.getName().startsWith(CallbacksPerTestTest.class.getSimpleName()))
                .collect(Collectors.toSet()); //todo only if there is onw

        if (testDirs.size() > 1) {
            //todo log
            return -1;
        }
        if (testDirs.isEmpty()) {
            //todo log
            return 0;
        }

        return Arrays.stream(testDirs.stream().findFirst().get().listFiles())
                .filter(f -> f.getName().startsWith(callback.name()))
                .count();
    }
}
