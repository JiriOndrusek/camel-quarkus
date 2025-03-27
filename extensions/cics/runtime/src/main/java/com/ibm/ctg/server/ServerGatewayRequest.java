package com.ibm.ctg.server;

import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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

    public void execute() {
        Log.error("Not supported in native");
    }
}
