package org.apache.camel.quarkus.component.crypto.it.bc;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.function.Supplier;

public class BcTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(RouteBuilder.class)
                            .add(new StringAsset(
                                            ContinuousTestingTestUtils.appProperties("#")),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FirstET.class, SecondET.class);
                }
            });
}
