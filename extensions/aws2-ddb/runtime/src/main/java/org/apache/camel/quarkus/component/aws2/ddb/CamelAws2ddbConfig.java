package org.apache.camel.quarkus.component.aws2.ddb;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.aws2ddb", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelAws2ddbConfig {

    //    @ConfigItem
    //    public Optional<Region> region;
    //    @ConfigItem
    //    public AwsCredentialsProviderConfig credentials;
}
