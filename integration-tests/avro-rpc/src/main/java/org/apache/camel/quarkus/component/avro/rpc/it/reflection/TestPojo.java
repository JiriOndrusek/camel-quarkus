package org.apache.camel.quarkus.component.avro.rpc.it.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TestPojo {

    private String pojoName;

    public String getPojoName() {
        return pojoName;
    }

    public void setPojoName(String pojoName) {
        this.pojoName = pojoName;
    }
}
