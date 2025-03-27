package com.ibm.ctg.client.management;

import java.util.Arrays;
import java.util.ResourceBundle;

public class CTGCtrlUtil {

    public static void getMsg(ResourceBundle var1, String var2, Object[] var3) {
        io.quarkus.logging.Log.errorf("Logged error: %s, %d, %s", var1, var2, Arrays.toString(var3));
    }
}
