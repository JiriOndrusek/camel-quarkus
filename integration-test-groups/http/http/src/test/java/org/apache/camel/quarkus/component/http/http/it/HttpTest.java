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
package org.apache.camel.quarkus.component.http.http.it;

import java.security.KeyPair;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.component.http.common.HttpTestResource;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPair;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPairs;
import org.apache.camel.quarkus.test.support.pqc.certificate.CertificateFormat;
import org.apache.camel.quarkus.test.support.pqc.certificate.HybridMode;
import org.apache.camel.quarkus.test.support.pqc.certificate.PQCCertificate;
import org.apache.camel.quarkus.test.support.pqc.certificate.PQCCertificates;
import org.apache.camel.quarkus.test.support.pqc.certificate.PrimaryAlgorithm;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@TestCertificates(certificates = {
        @Certificate(name = HttpTestResource.KEYSTORE_NAME, formats = {
                Format.PKCS12 }, password = HttpTestResource.KEYSTORE_PASSWORD) })
@PQCCertificates(baseDir = "target/certs/bctls-nginx", certificates = {
        @PQCCertificate(name = "nginx-hybrid-pqc", hybridMode = HybridMode.CHIMERA, primaryAlgorithm = PrimaryAlgorithm.RSA_2048, pqcAlgorithm = PQCAlgorithm.DILITHIUM2, cn = "nginx-hybrid-pqc", validity = 30, formats = {
                CertificateFormat.PEM, CertificateFormat.PKCS12 }, password = "changeit")
})
@PQCKeyPairs(keyPairs = {
        @PQCKeyPair(name = "dilithiumKeyPair", algorithm = PQCAlgorithm.DILITHIUM2),
})
@QuarkusTest
@QuarkusTestResource(PqcNginxTestResource.class)
public class HttpTest {

    @Inject
    @jakarta.inject.Named("dilithiumKeyPair")
    KeyPair dilithiumKeyPair;

    @Test
    public void testPqcNginxTls() {
        // Test BCTLS integration with hybrid RSA+Dilithium certificate
        // Certificate contains both RSA (for TLS handshake) and Dilithium2 (alternative signature)
        // Following BC Almanac Chimera-style composite certificate recommendations
        // Note: Dilithium is the legacy name; ML-DSA is the NIST standardized name
        RestAssured
                .when()
                .get("/test/client/http/pqc/nginx/tls")
                .then()
                .statusCode(200)
                .body(is("Hybrid RSA+Dilithium(ML-DSA) certificate validated"));
    }

}
