package com.ibm.as400.access;

import java.nio.charset.StandardCharsets;

public class ReplyRCCallProgram extends RCCallProgramReplyDataStream {

    public ReplyRCCallProgram() {
    }

    @Override
    int getRC() {
        //success
        return 0x0000;
    }

    @Override
    void getParameterList(ProgramParameter[] parameterList) {
        //skip
        byte[] str = "hello".repeat(50).getBytes(StandardCharsets.UTF_8);
        //lengths are encoded in the string
        //        int lengthDataReturned = BinaryConverter.byteArrayToInt(data, 152);
        //        int lengthMessageReturned = BinaryConverter.byteArrayToInt(data, 160);
        //        int lengthHelpReturned = BinaryConverter.byteArrayToInt(data, 168);
        str[150] = 0;
        str[151] = 0;
        str[152] = 0;
        str[153] = 0;
        str[154] = 0;
        str[155] = 1;
        str[156] = 0;
        str[157] = 0;
        str[158] = 0;
        str[159] = 0;
        str[160] = 0;
        str[161] = 0;
        str[162] = 0;
        str[163] = 0;
        str[164] = 0;
        str[165] = 0;
        str[166] = 0;
        str[167] = 1;
        str[168] = 0;
        str[169] = 0;
        str[170] = 0;
        str[171] = 0;
        str[172] = 0;
        str[173] = 0;
        str[174] = 0;
        str[175] = 1;
        str[176] = 0;

        parameterList[0].setOutputData(str);
    }
}
