package com.ibm.as400.access;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockAS400 extends AS400 {

    @Override
    public AS400Impl getImpl() {
        return new MockAS400ImplRemote();
    }

    //    @Override
    //    Object loadImpl2(String impl1, String impl2) {
    //        return super.loadImpl2(impl1, impl2);
    //    }

    //    @Override
    //    synchronized void signon(boolean keepConnection) throws AS400SecurityException, IOException {
    //        //do nothing
    //        System.out.println("SIGNON");
    //    }

    //    @Override
    //    synchronized void signon(boolean keepConnection) throws AS400SecurityException, IOException {
    //        super.signon(keepConnection);
    //    }
    //
    @Override
    public void connectService(int service, int overridePort) throws AS400SecurityException, IOException {
        //do nothing
        System.out.println("connect service");
    }
}
