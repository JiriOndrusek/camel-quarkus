package org.apache.camel.quarkus.component.as2.it.transport;

public class ServerResult {

    String result;
    String requestClass;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRequestClass() {
        return requestClass;
    }

    public void setRequestClass(String requestClass) {
        this.requestClass = requestClass;
    }
}
