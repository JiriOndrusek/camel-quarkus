package org.apache.camel.quarkus.component.jt400.it;

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
}
