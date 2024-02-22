package com.ibm.as400.access;

import java.nio.charset.StandardCharsets;

public class NormalReply extends DQReadNormalReplyDataStream {

//    private final int hashCode;
//    private final String senderInformation;
//    private final String entry;
//    private final String key;
//
//    public NormalReply(int hashCode, String senderInformation, String entry, String key) {
//        this.hashCode = hashCode;
//        this.senderInformation = senderInformation;
//        this.entry = entry;
//        this.key = key;
//    }

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
