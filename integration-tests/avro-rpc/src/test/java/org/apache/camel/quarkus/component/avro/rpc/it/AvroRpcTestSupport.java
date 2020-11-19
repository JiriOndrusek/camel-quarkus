/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.avro.rpc.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.avro.ipc.Requestor;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.Key;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.generated.Value;
import org.apache.camel.quarkus.component.avro.rpc.it.impl.KeyValueProtocolImpl;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestPojo;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(AvroRpcTestResource.class)
abstract class AvroRpcTestSupport {

    private final String NAME = "Sheldon";

    private TestReflection testReflection;

    private KeyValueProtocol keyValueProtocol;

    private final ProtocolType protocol;

    private Requestor reflectRequestor;

    public AvroRpcTestSupport(ProtocolType protocol) {
        this.protocol = protocol;
    }

    @Test
    public void testReflectionProducer() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .body(NAME)
                .post("/avro-rpc/reflectionProducer")
                .then()
                .statusCode(204);
        assertEquals(NAME, testReflection.getName());
    }

    @Test
    public void testGeneratedProducer() throws InterruptedException {
        Key key = Key.newBuilder().setKey("1").build();
        Value value = Value.newBuilder().setValue(NAME).build();

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .queryParam("key", key.getKey().toString())
                .body(value.getValue().toString())
                .post("/avro-rpc/generatedProducerPut")
                .then()
                .statusCode(204);

        assertEquals(value, ((KeyValueProtocolImpl) keyValueProtocol).getStore().get(key));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .body(key.getKey().toString())
                .post("/avro-rpc/generatedProducerGet")
                .then()
                .statusCode(200)
                .body(is("{\"value\": \"" + NAME + "\"}"));
    }

    void testReflectionConsumer() throws Exception {
        TestPojo testPojo = new TestPojo();
        testPojo.setPojoName(NAME);
        Object[] request = { testPojo };
        reflectRequestor.request("setTestPojo", request);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(protocol)
                .post("/avro-rpc/reflectionConsumer")
                .then()
                .statusCode(200)
                .body(is(NAME));
    }

    void setTestReflection(TestReflection testReflection) {
        this.testReflection = testReflection;
    }

    public void setKeyValueProtocol(KeyValueProtocol keyValueProtocol) {
        this.keyValueProtocol = keyValueProtocol;
    }

    void setReflectRequestor(Requestor reflectRequestor) {
        this.reflectRequestor = reflectRequestor;
    }

    boolean isHttp() {
        return ProtocolType.http == protocol;
    }
}
