package com.ibm.as400.access;

public class ReplyRCExchangeAttributes extends RCExchangeAttributesReplyDataStream {

    @Override
    int getRC() {
        //successful return code
        return 0x0000;
    }

    @Override
    int getCCSID() {
        //ConvTable37 depends on this value
        return 37;
    }

    // Server datastream level.
    int getDSLevel() {
        return 1;
    }
}
