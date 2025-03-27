package com.ibm.ctg.server;

import io.quarkus.logging.Log;

public class ServerGatewayRequest {

    public void setLocalMode(boolean b) {
        Log.error("Not supported in native");
    }

    public static void executeCleanup() {
        Log.error("Not supported in native");
    }

    public static void clientDisconnected() {
        Log.error("Not supported in native");
    }

    public static void execute() {
        Log.error("Not supported in native");
    }
}
