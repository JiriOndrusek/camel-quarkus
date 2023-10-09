package org.apache.camel.quarkus.component.tika;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Tika parser configuration
 */
@ConfigRoot(name = "camel.tika", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TikaConfig {

    /**
     *
     * todo use set of regxps
     */
    @ConfigItem
    public Optional<Set<String>> include;
    /**
     * todo
     */
    @ConfigItem
    public Optional<Set<String>> exclude;
}
