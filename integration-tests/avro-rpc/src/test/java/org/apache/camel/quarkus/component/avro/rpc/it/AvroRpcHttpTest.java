package org.apache.camel.quarkus.component.avro.rpc.it;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AvroRpcHttpTest extends AvroRpcTestSupport {

    public AvroRpcHttpTest() {
        super(ProtocolType.http);
    }
}
