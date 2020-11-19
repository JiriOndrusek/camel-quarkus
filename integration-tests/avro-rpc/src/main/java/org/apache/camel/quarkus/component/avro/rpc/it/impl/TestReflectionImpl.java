package org.apache.camel.quarkus.component.avro.rpc.it.impl;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestPojo;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;

@RegisterForReflection(methods = true)
public class TestReflectionImpl implements TestReflection {

    String name = "";
    int age;
    TestPojo testPojo;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getAge() {
        return this.age;
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public int increaseAge(int age) {
        this.age = ++age;
        return this.age;
    }

    @Override
    public void setTestPojo(TestPojo testPojo) {
        this.testPojo = testPojo;
    }

    @Override
    public TestPojo getTestPojo() {
        return testPojo;
    }

}
