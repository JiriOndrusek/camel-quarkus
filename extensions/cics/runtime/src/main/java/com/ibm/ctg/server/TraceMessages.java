package com.ibm.ctg.server;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TraceMessages {

    public static String getMessage(int var1) {
        return "Unsupported in native (input was " + var1 + ")";
    }
}
