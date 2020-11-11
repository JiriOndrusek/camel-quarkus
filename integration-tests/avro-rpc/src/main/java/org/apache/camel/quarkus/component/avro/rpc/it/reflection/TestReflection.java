package org.apache.camel.quarkus.component.avro.rpc.it.reflection;

public interface TestReflection {

    String getName();

    void setName(String name);

    int getAge();

    void setAge(int age);

    int increaseAge(int age);

    void setTestPojo(TestPojo testPojo);

    TestPojo getTestPojo();

}
