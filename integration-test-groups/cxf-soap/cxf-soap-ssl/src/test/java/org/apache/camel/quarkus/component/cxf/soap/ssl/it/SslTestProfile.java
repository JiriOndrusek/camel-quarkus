package org.apache.camel.quarkus.component.cxf.soap.ssl.it;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.commons.lang3.RandomStringUtils;

public class SslTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return RandomStringUtils.randomAlphanumeric(16);
    }
}
