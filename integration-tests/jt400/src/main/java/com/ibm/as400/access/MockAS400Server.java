package com.ibm.as400.access;

import java.io.IOException;

public class MockAS400Server extends AS400NoThreadServer {
    MockAS400Server(AS400ImplRemote system) throws IOException {
        super(system, 1, new MockSocketContainer(), "jobString");
    }

    @Override
    public DataStream sendAndReceive(DataStream requestStream) throws IOException {
        if (requestStream.data_.length == 26)
            return new Reply1();
        if (requestStream.data_.length == 59)
            return new Reply3();

        return new Reply2();
    }

}
