package org.apache.camel.quarkus.component.debezium.postgres.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.slf4j.Logger;

@TargetClass(io.debezium.metrics.Metrics.class)
final class SubstituteMetrics {

    @Substitute
    public synchronized void register(Logger logger) {
        logger.debug("Metrics are not registered in native mode.");
    }

    @Substitute
    public final void unregister(Logger logger) {
        logger.debug("Metrics are not unregistered in native mode.");
    }
}
