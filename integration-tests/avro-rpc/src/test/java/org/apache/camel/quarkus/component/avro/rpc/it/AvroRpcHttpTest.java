package org.apache.camel.quarkus.component.avro.rpc.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AvroRpcHttpTest extends AvroRpcTestSupport {

    public AvroRpcHttpTest() {
        super(ProtocolType.http);
    }

    @Test
    public void testReflectionConsumer() throws Exception {
        super.testReflectionConsumer();
    }
}
