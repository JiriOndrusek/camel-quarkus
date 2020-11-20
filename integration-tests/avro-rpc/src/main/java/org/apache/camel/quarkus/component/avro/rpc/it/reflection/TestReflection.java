package org.apache.camel.quarkus.component.avro.rpc.it.reflection;

public interface TestReflection {

    String getName();

    void setName(String name);

    void setTestPojo(TestPojo testPojo);

    TestPojo getTestPojo();

}
