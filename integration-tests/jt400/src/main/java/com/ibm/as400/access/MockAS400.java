package com.ibm.as400.access;

import java.io.IOException;

public class MockAS400 extends AS400 {

    private final MockAS400ImplRemote as400ImplRemote;

    public MockAS400(MockAS400ImplRemote as400ImplRemote) {
        this.as400ImplRemote = as400ImplRemote;
    }

    @Override
    public AS400Impl getImpl() {
        return as400ImplRemote;
    }

    @Override
    public int getCcsid() {
        //ConvTable37 depends on this value
        return 37;
    }

    @Override
    public boolean isConnected(int service) {
        //always connected
        return true;
    }

    @Override
    public void connectService(int service, int overridePort) throws AS400SecurityException, IOException {
        //connection to real i server is ignored
        setSignonInfo(-1, -1, "username");
    }

    @Override
    synchronized void signon(boolean keepConnection) throws AS400SecurityException, IOException {
        //do nothing
    }

    @Override
    public int getVRM() throws AS400SecurityException, IOException {
        return 1;
    }
}
