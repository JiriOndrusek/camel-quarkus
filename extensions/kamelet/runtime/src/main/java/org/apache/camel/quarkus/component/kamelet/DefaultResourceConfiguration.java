package org.apache.camel.quarkus.component.kamelet;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.spi.Resource;

public class DefaultResourceConfiguration implements Resource {

    private String location;

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    public DefaultResourceConfiguration setLocation(String location) {
        this.location = location;
        return this;
    }

}
