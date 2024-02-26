package com.ibm.as400.access;

public class DQCommonReply extends DQCommonReplyDataStream {

    private final int hashCode;

    public DQCommonReply(int hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    int getRC() {
        //because of https://github.com/IBM/JTOpen/blob/main/src/main/java/com/ibm/as400/access/BaseDataQueueImplRemote.java#L332
        return 0xF000;
    }
}
