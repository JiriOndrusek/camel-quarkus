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
package org.apache.camel.quarkus.component.pqc.it;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPair;
import org.apache.camel.quarkus.test.support.pqc.PQCKeyPairs;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.jboss.logging.Logger;

import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.DILITHIUM2;
import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.FALCON512;
import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.KYBER512;
import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.LMS;
import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.SPHINCSPLUS;
import static org.apache.camel.quarkus.test.support.pqc.PQCAlgorithm.XMSS;

@Path("/pqc")
@ApplicationScoped
@PQCKeyPairs(keyPairs = {
        @PQCKeyPair(name = "dilithiumKeyPair", algorithm = DILITHIUM2),
        @PQCKeyPair(name = "falconKeyPair", algorithm = FALCON512),
        @PQCKeyPair(name = "sphincsKeyPair", algorithm = SPHINCSPLUS),
        @PQCKeyPair(name = "xmssKeyPair", algorithm = XMSS),
        @PQCKeyPair(name = "lmsKeyPair", algorithm = LMS),
        @PQCKeyPair(name = "kyberKeyPair", algorithm = KYBER512)
})
public class PqcResource {

    private static final Logger LOG = Logger.getLogger(PqcResource.class);;

    @Inject
    @Named("kyberKeyPair")
    KeyPair kyberKeyPair;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/sign/{algorithm}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String sign(String message, @PathParam("algorithm") String algorithm) {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=%s&keyPair=%s".formatted(algorithm, toKeyPair(algorithm)),
                ex -> ex.getIn().setBody(message.getBytes(StandardCharsets.UTF_8)));

        // The sign operation outputs signature in the HEADER, not the body
        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/{algorithm}/{message}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verify(String signature,
            @PathParam("algorithm") String algorithm,
            @PathParam("message") String message) {
        byte[] signatureBytes = Base64.getDecoder().decode(signature);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=%s&keyPair=%s".formatted(algorithm, toKeyPair(algorithm)),
                ex -> {
                    ex.getIn().setBody(message.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/kem/encapsulate/{algorithm}/{keyAlgorithm}/{length}")
    @POST
    @jakarta.ws.rs.Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> encapsulate(@PathParam("algorithm") String algorithm,
            @PathParam("keyAlgorithm") String keyAlgorithm,
            @PathParam("length") int length) {
        SecretKeyWithEncapsulation secretKeyWithEncapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=%s&symmetricKeyAlgorithm=%s&symmetricKeyLength=%s&keyPair=%s"
                        .formatted(algorithm, keyAlgorithm, length, toKeyPair(algorithm)),
                null,
                SecretKeyWithEncapsulation.class);
        String enc = Base64.getEncoder().encodeToString(secretKeyWithEncapsulation.getEncapsulation());
        String secret = Base64.getEncoder().encodeToString(secretKeyWithEncapsulation.getEncoded());
        return Map.of("enc", enc, "secret", secret);
    }

    @Path("/kem/extract/{algorithm}/{keyAlgorithm}/{length}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String extract(String enc,
            @PathParam("algorithm") String algorithm,
            @PathParam("keyAlgorithm") String keyAlgorithm,
            @PathParam("length") int length) {

        byte[] encapsulation = Base64.getDecoder().decode(enc);
        //create SecretKeyWithEncapsulation from the private key
        SecretKeyWithEncapsulation privateKeyWithEncapsulation = new SecretKeyWithEncapsulation(
                new SecretKeySpec(kyberKeyPair.getPrivate().getEncoded(), algorithm), encapsulation);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=%s&symmetricKeyAlgorithm=%s&symmetricKeyLength=%s&keyPair=%s"
                        .formatted(algorithm, keyAlgorithm, length, toKeyPair(algorithm)),
                privateKeyWithEncapsulation,
                SecretKeyWithEncapsulation.class);
        return Base64.getEncoder().encodeToString(result.getEncoded());
    }

    private String toKeyPair(String algorithm) {
        return "SPHINCSPLUS".equals(algorithm) ? "#sphincsKeyPair" : "#" + algorithm.toLowerCase() + "KeyPair";
    }

    // Body tests: binary data
    @Path("/signBinaryData")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithBinaryData(String message) {
        byte[] binaryData = message.getBytes(StandardCharsets.UTF_8);

        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(binaryData));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        // Store binary data for verification
        exchange.getIn().setHeader("CamelTestBinaryData", binaryData);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verifyBinaryData/{message}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithBinaryData(String signature,
            @PathParam("message") String message) {
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        byte[] binaryData = message.getBytes(StandardCharsets.UTF_8);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> {
                    ex.getIn().setBody(binaryData);
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Native mode test: check provider
    @Path("/provider/check")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String checkProvider() {
        java.security.Provider[] providers = Security.getProviders();
        for (java.security.Provider provider : providers) {
            if ("BCPQC".equals(provider.getName())) {
                return "available";
            }
        }
        return "unavailable";
    }

}
