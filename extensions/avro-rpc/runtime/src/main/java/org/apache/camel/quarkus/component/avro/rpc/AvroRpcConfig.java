package org.apache.camel.quarkus.component.avro.rpc;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.avro", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AvroRpcConfig {

    /**
     * Sets path for http servlet mapping for specific mapping.
     * If both specific and reflect mappings are empty, all paths are handled.
     * todo
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> httpSpecificMapping;

    /**
     * Sets path for http servlet mapping for reflect mapping.
     * If both specific and reflect mappings are empty, all paths are handled.
     * * todo
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> httpReflectMapping;
}
