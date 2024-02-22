package org.apache.camel.quarkus.component.jt400.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.as400.access.AS400ConnectionPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.quarkus.component.jt400.it.mock.MockAS400ConnectionPool;

public class Jt400Producers {

    @Produces
    @ApplicationScoped
    @Named("mockPool")
    AS400ConnectionPool produceConnectionPool() {
        return new MockAS400ConnectionPool();
    }

    @Produces
    @ApplicationScoped
    @Named("collected-data")
    public Map<String, List<String>> collectedData() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("queue", new CopyOnWriteArrayList<>());
        return result;
    }
}
