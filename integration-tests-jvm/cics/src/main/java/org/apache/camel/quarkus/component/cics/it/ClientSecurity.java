package org.apache.camel.quarkus.component.cics.it;

import java.io.IOException;
import java.net.InetAddress;

import com.ibm.ctg.client.GatewayRequest;

public class ClientSecurity implements com.ibm.ctg.security.ClientSecurity {
    @Override
    public byte[] generateHandshake(InetAddress inetAddress) throws IOException {
        return new byte[0];
    }

    @Override
    public void repliedHandshake(byte[] bytes) throws IOException {

    }

    @Override
    public byte[] encodeRequest(byte[] bytes, GatewayRequest gatewayRequest) throws IOException {
        return new byte[0];
    }

    @Override
    public byte[] decodeReply(byte[] bytes) throws IOException {
        return new byte[0];
    }

    @Override
    public void afterDecode(GatewayRequest gatewayRequest) {

    }
}
