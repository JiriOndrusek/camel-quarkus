package com.ibm.as400.access;

public class MockDQRequestAttributesNormalReplyDataStream extends DQRequestAttributesNormalReplyDataStream {

    @Override
    public int hashCode() {
        return 0x8000;
    }

    @Override
    int getType() {
        //required for keyed
        return 2;
    }

    @Override
    int getMaxEntryLength() {
        return 1;
    }
}
