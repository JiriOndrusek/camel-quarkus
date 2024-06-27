package org.apache.camel.quarkus.component.crypto.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CryptoBcProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.security.security-providers", "BC");
    }
}
