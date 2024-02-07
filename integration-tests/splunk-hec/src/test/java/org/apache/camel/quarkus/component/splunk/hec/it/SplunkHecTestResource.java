package org.apache.camel.quarkus.component.splunk.hec.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.camel.quarkus.test.support.splunk.SplunkTestResource;

public class SplunkHecTestResource extends SplunkTestResource {

    private static final String SERVER_CERTIFICATE__FILE = "/localhost.pem";
    private static final String CA_CERTIFICATE_FILE = "/ca.pem";

    @Override
    public byte[] getCaCertPath() throws IOException {
        return Files.readAllBytes(Path.of(SplunkHecTestResource.class.getResource(SERVER_CERTIFICATE__FILE).getPath()));
    }

    @Override
    public byte[] getServerCertPath() throws IOException {
        return Files.readAllBytes(Path.of(SplunkHecTestResource.class.getResource(CA_CERTIFICATE_FILE).getPath()));
    }
}
