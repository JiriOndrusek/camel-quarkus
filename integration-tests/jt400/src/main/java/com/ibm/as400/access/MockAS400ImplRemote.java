package com.ibm.as400.access;

import java.io.IOException;

public class MockAS400ImplRemote extends AS400ImplRemote {

    AS400Server getConnection(int service, boolean forceNewConnection,
            boolean skipSignonServer) throws AS400SecurityException, IOException {
        return new MockAS400Server(this);
    }

    @Override
    public String getNLV() {
        return "012345678901234567890123456789";
    }
}
