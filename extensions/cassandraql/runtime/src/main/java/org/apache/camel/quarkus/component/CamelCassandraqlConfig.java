package org.apache.camel.quarkus.component;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "cassandra.init", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelCassandraqlConfig {

    /**
     * Reads configuration of cassandra-quarkus init property eager-session-init.
     */
    @ConfigItem(name = "eager-session-init")
    public Optional<Boolean> eagerSessionInit;
}
