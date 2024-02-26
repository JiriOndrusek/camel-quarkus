package com.ibm.as400.access;

import java.io.IOException;

public class MockAS400Server extends AS400NoThreadServer {

    MockAS400Server(AS400ImplRemote system) throws IOException {
        super(system, 1, new MockSocketContainer(), "job/String/something");
    }

    @Override
    public DataStream sendAndReceive(DataStream requestStream) throws IOException {
        if (!MockedResponses.isEmpty()) {
            return MockedResponses.removeFirst();
        }

        return null;
    }

}
