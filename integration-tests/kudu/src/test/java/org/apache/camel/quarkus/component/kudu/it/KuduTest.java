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
package org.apache.camel.quarkus.component.kudu.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.kdc.AbstractKerberosITest;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.test.KuduTestHarness;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

//@RunWith(FrameworkRunner.class)
//@CreateDS(name = "Krb5LoginModuleTest-class",
//        partitions =
//                {
//                        @CreatePartition(
//                                name = "example",
//                                suffix = "dc=example,dc=com",
//                                contextEntry = @ContextEntry(
//                                        entryLdif =
//                                                "dn: dc=example,dc=com\n" +
//                                                        "dc: example\n" +
//                                                        "objectClass: top\n" +
//                                                        "objectClass: domain\n\n"),
//                                indexes =
//                                        {
//                                                @CreateIndex(attribute = "objectClass"),
//                                                @CreateIndex(attribute = "dc"),
//                                                @CreateIndex(attribute = "ou")
//                                        })
//                },
//        additionalInterceptors =
//                {
//                        KeyDerivationInterceptor.class
//                })
////@CreateLdapServer(
////        transports =
////                {
////                        @CreateTransport(protocol = "LDAP")
////                },
////        saslHost = "localhost",
////        saslPrincipal = "ldap/localhost@EXAMPLE.COM",
////        saslMechanisms =
////                {
////                        @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
////                        @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
////                        @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
////                        @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
////                        @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
////                        @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
////                })
//@CreateKdcServer(
//        transports =
//                {
//                        @CreateTransport(protocol = "UDP", port = 6088),
//                        @CreateTransport(protocol = "TCP", port = 6088)
//                })
//@ApplyLdifs({
//        "dn: ou=users,dc=example,dc=com",
//        "objectClass: top",
//        "objectClass: organizationalUnit",
//        "ou: users"
//})
@QuarkusTestResource(KuduTestResource.class)
@QuarkusTest
class KuduTest extends AbstractKerberosITest {
    private static final Logger LOG = Logger.getLogger(KuduTest.class);
    private KuduClient client;

    @BeforeEach
    void beforeEach() throws KuduException {
        createTable();
    }

    @AfterEach
    void afterEach() {
        if (client != null) {
            try {
                client.deleteTable(KuduRoute.TABLE_NAME);
            } catch (KuduException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void createTable() throws KuduException {
        assertEquals(0, client.getTablesList().getTablesList().size());
        RestAssured.put("/kudu/createTable")
                .then()
                .statusCode(200);
        assertEquals(1, client.getTablesList().getTablesList().size());
    }

    @Test
    @KuduTestHarness.EnableKerberos(principal = "kuduuser")
    void kuduCrud() throws KuduException {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));

        // Update
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "John", "age", 40))
                .patch("/kudu/update")
                .then()
                .statusCode(200);

        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("John"),
                        "[0].age", is(40));

        // Delete
        RestAssured.delete("/kudu/delete/key1")
                .then()
                .statusCode(200);

        // Confirm deletion
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void upsertUpdate() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));

        // Upsert update of name
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "John", "age", 50))
                .patch("/kudu/upsert")
                .then()
                .statusCode(200);

        // Read update
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("John"),
                        "[0].age", is(50));
    }

    @Test
    void upsertInsert() {
        // Upsert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));
    }

    @Test
    void scanWithSpecificColumns() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.given()
                .queryParam("columnNames", "name,age")
                .get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", nullValue(),
                        "[0].age", equalTo(50),
                        "[0].name", is("Samuel"));
    }

    @Test
    void scanWithPredicate() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key2", "name", "Alice", "age", 12))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key3", "name", "John", "age", 40))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read (finds all records with age >= 18)
        RestAssured.given()
                .queryParam("minAge", 18)
                .get("/kudu/scan/predicate")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50),
                        "[1].id", is("key3"),
                        "[1].name", is("John"),
                        "[1].age", is(40));
    }

    @Test
    void scanWithLimit() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key2", "name", "John", "age", 40))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key3", "name", "Alice", "age", 12))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read (limit of 2 records)
        RestAssured.given()
                .queryParam("limit", 2)
                .get("/kudu/scan/limit")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50),
                        "[1].id", is("key2"),
                        "[1].name", is("John"),
                        "[1].age", is(40));
    }

    void setClient(KuduClient client) {
        this.client = client;
    }
}
