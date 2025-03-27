
package com.ibm.ctg.server.logging;

import java.util.Arrays;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Class com.ibm.ctg.server.logging.Log is referenced from com.ibm.ctg.monitoring.RequestExitMonitor. Class is not part
 * of the ctgClient.
 * It seems, that class belongs to the server side of the ctg.
 * This simple implementation is replacing it fully.
 */
@RegisterForReflection
public class Log {

    public static void printErrorLn(String var1, int var2, Object[] var3) {
        io.quarkus.logging.Log.errorf("Logged error: %s, %d, %s", var1, var2, Arrays.toString(var3));
    }

    public static void printInfoLn(String var1, int var2, Object[] var3) {
        io.quarkus.logging.Log.errorf("Logged error: %s, %d, %s", var1, var2, Arrays.toString(var3));
    }
}
