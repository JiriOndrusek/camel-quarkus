package org.apache.camel.quarkus.component.avro.rpc.it.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.quarkus.component.avro.rpc.it.generated.Key;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.Value;

public class KeyValueProtocolImpl implements KeyValueProtocol {

    private Map<Key, Value> store = new HashMap<>();

    @Override
    public void put(Key key, Value value) {
        store.put(key, value);
    }

    @Override
    public Value get(Key key) {
        return store.get(key);
    }

    public Map<Key, Value> getStore() {
        return store;
    }

}
