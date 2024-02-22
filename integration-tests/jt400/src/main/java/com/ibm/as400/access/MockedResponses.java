package com.ibm.as400.access;

import java.util.LinkedList;

public class MockedResponses {

    private static LinkedList<DataStream> responses = new LinkedList<>();

    public static void add(DataStream dataStream) {
        responses.add(dataStream);
    }

    public static DataStream removeFirst() {
        return responses.removeFirst();
    }
}
