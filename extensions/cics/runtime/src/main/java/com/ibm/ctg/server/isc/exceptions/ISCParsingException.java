package com.ibm.ctg.server.isc.exceptions;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ISCParsingException extends Exception {

    public ISCParsingException(String message) {
        super(message);
    }
}
