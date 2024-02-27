package com.ibm.as400.access;

import java.nio.charset.StandardCharsets;

public class ReplyDQReadNormal extends DQReadNormalReplyDataStream {

    private final int hashCode;
    private final String senderInformation;
    private final String entry;
    private final String key;

    public ReplyDQReadNormal(int hashCode, String senderInformation, String entry, String key) {
        this.hashCode = hashCode;
        this.senderInformation = senderInformation;
        this.entry = entry;
        this.key = key;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    byte[] getSenderInformation() {
        return senderInformation.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    byte[] getEntry() {
        return entry.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    byte[] getKey() {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
