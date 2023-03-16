package org.apache.camel.quarkus.component.cxf.soap.ssl.it;

public class GreeterImpl implements GreeterService {
    @Override
    public String greetMe(String name) {
        return "Hello " + name + "!";
    }
}
