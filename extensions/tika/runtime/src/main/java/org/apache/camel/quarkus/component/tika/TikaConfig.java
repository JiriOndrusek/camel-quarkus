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
     * The resource path within the application artifact to the {@code tika-config.xml} file.
     */
    @ConfigItem
    public Optional<String> tikaConfigPath;

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

    /**
     * Controls how the content of the embedded documents is parsed.
     * By default it is appended to the main document content.
     * Setting this property to false makes the content of each of the embedded documents
     * available separately.
     */
    @ConfigItem(defaultValue = "true")
    public boolean appendEmbeddedContent;
}
