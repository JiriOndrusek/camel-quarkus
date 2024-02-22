package com.ibm.as400.access;

import java.nio.charset.StandardCharsets;

public class Reply3 extends DQReadNormalReplyDataStream {

    @Override
    public int hashCode() {
        return 0x8003;
    }

    @Override
    byte[] getSenderInformation() {
        return "Reply3".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    byte[] getEntry() {
        return "Hello from mocked jt400!".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    byte[] getKey() {
        return "MYKEY".getBytes(StandardCharsets.UTF_8);
    }
}
