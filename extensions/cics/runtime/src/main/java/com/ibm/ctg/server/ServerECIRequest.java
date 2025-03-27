package com.ibm.ctg.server;

import io.quarkus.logging.Log;

public class ServerECIRequest {

    public static void initializeLocalModeJNI() {
        Log.error("Not supported in native");
    }
}
