package com.ibm.as400.access;

import java.io.IOException;

public class MockAS400Server extends AS400NoThreadServer {
    MockAS400Server(AS400ImplRemote system) throws IOException {
        super(system, 1, new MockSocketContainer(), "jobString");
    }

    //    @Override
    //    int getService() {
    //        return 0;
    //    }
    //
    //    @Override
    //    String getJobString() {
    //        return null;
    //    }
    //
    //    @Override
    //    boolean isConnected() {
    //        return false;
    //    }
    //
    //    @Override
    //    public DataStream getExchangeAttrReply() {
    //        return new MockDataStream();
    //    }
    //
    //    @Override
    //    public DataStream sendExchangeAttrRequest(DataStream req) throws IOException, InterruptedException {
    //        return null;
    //    }
    //
    //    @Override
    //    void addInstanceReplyStream(DataStream replyStream) {
    //
    //    }
    //
    //    @Override
    //    void clearInstanceReplyStreams() {
    //
    //    }
    //
    @Override
    public DataStream sendAndReceive(DataStream requestStream) throws IOException {
        if (requestStream.data_.length == 26)
            return new MockDQRequestAttributesNormalReplyDataStream();
        if (requestStream.data_.length == 59)
            return new Reply3();

        return new Reply2();
    }
    //
    //    @Override
    //    void sendAndDiscardReply(DataStream requestStream) throws IOException {
    //
    //    }
    //
    //    @Override
    //    void sendAndDiscardReply(DataStream requestStream, int correlationId) throws IOException {
    //
    //    }
    //
    //    @Override
    //    int send(DataStream requestStream) throws IOException {
    //        return 0;
    //    }
    //
    //    @Override
    //    int newCorrelationId() {
    //        return 0;
    //    }
    //
    //    @Override
    //    void send(DataStream requestStream, int correlationId) throws IOException {
    //
    //    }
    //
    //    @Override
    //    DataStream receive(int correlationId) throws IOException, InterruptedException {
    //        return null;
    //    }
    //
    //    @Override
    //    void forceDisconnect() {
    //
    //    }
    //
    //    @Override
    //    void setSoTimeout(int timeout) throws SocketException {
    //
    //    }
    //
    //    @Override
    //    int getSoTimeout() throws SocketException {
    //        return 0;
    //    }
}
