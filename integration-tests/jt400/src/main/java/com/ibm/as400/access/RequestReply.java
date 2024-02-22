package com.ibm.as400.access;

public class RequestReply extends DQRequestAttributesNormalReplyDataStream {

    @Override
    public int hashCode() {
        return 0x8001;
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

    @Override
    boolean getSaveSenderInformation() {
        return false;
    }

    @Override
    boolean getForceToAuxiliaryStorage() {
        return false;
    }

    @Override
    byte[] getDescription() {
        return new byte[5];
    }

    @Override
    int getKeyLength() {
        //because of MYKEY
        return 5;
    }
}
