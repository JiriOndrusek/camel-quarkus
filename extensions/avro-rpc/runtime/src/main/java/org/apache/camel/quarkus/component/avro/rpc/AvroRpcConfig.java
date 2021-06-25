package org.apache.camel.quarkus.component.avro.rpc;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.avro", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AvroRpcConfig {

    /**
     * Sets path for http servlet mapping to listen on. Default is /*
     */
    @ConfigItem(defaultValue = "/*")
    public String httpServletMapping;
}
