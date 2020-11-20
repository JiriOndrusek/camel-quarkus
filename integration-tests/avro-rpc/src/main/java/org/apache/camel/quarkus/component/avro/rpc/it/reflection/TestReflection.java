package org.apache.camel.quarkus.component.avro.rpc.it.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public interface TestReflection {

    String getName();

    void setName(String name);

    void setTestPojo(TestPojo testPojo);

    TestPojo getTestPojo();

}
