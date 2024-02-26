package com.ibm.as400.access;

public class MockDataStream extends DataStream {
    MockDataStream() {
        super(30);
    }

    @Override
    int getCorrelation() {
        return 0;
    }

    @Override
    int getLength() {
        return 0;
    }

    @Override
    void setCorrelation(int correlation) {

    }

    @Override
    void setLength(int len) {

    }
}
