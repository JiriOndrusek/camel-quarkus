package org.apache.camel.quarkus.component.avro.rpc.it;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AvroRpcNettyTest extends AvroRpcTestSupport {

    public AvroRpcNettyTest() {
        super(ProtocolType.netty);
    }
}
