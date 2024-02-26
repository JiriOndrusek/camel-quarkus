package com.ibm.as400.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class MockSocketContainer extends SocketContainer {

    ByteArrayOutputStream bOutput = new ByteArrayOutputStream(50);

    byte[] _data = new byte[50];

    public MockSocketContainer() {

        // https://github.com/IBM/JTOpen/blob/98e74fae6d212563a1558abce60ea5c73fcfc0c0/src/main/java/com/ibm/as400/access/ClientAccessDataStream.java#L70
        _data[6] = (byte) 0xE0;

        //sets length to 49
        _data[1] = 0;
        _data[2] = 0;
        _data[3] = '1';

        _data[4] = 0;
        _data[5] = 0;
        _data[7] = 0;
    }

    @Override
    void setProperties(Socket socket, String serviceName, String systemName, int port, SSLOptions options) throws IOException {

    }

    @Override
    void close() throws IOException {

    }

    @Override
    InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(_data);
    }

    @Override
    OutputStream getOutputStream() throws IOException {
        return bOutput;
    }

    @Override
    void setSoTimeout(int timeout) throws SocketException {

    }

    @Override
    int getSoTimeout() throws SocketException {
        return 0;
    }
}
