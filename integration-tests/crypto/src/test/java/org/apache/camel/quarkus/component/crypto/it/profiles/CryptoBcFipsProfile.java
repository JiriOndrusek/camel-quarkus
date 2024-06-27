package org.apache.camel.quarkus.component.crypto.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CryptoBcFipsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.security.security-providers", "BCFIPS");
    }
}
