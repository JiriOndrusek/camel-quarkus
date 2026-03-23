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
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.FalconParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.SPHINCSPlusParameterSpec;
import org.jboss.logging.Logger;

@Path("/pqc")
@ApplicationScoped
public class PqcResource {

    private static final Logger LOG = Logger.getLogger(PqcResource.class);
    private static final String TEST_MESSAGE = "Hello Camel Quarkus PQC";

    @Inject
    ProducerTemplate producerTemplate;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    // Register KeyPairs as CDI beans for Camel registry
    @Produces
    @Named("dilithiumKeyPair")
    public KeyPair dilithiumKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Named("falconKeyPair")
    public KeyPair falconKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_512, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Named("sphincsKeyPair")
    public KeyPair sphincsKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Named("kyberKeyPair")
    public KeyPair kyberKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        return gen.generateKeyPair();
    }

    // Sign operation using ML-DSA (Dilithium)
    @Path("/sign/dilithium")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithDilithium() {
        byte[] signature = producerTemplate.requestBody(
                "pqc:sign?operation=sign&signatureAlgorithm=Dilithium&keyPair=#dilithiumKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    // Verify operation using ML-DSA (Dilithium)
    @Path("/verify/dilithium")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithDilithium(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPqcSignature", signatureBytes);

        Boolean result = producerTemplate.requestBodyAndHeaders(
                "pqc:verify?operation=verify&signatureAlgorithm=Dilithium&keyPair=#dilithiumKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                headers,
                Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    // Sign operation using Falcon
    @Path("/sign/falcon")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithFalcon() {
        byte[] signature = producerTemplate.requestBody(
                "pqc:sign?operation=sign&signatureAlgorithm=Falcon&keyPair=#falconKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    // Verify operation using Falcon
    @Path("/verify/falcon")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithFalcon(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPqcSignature", signatureBytes);

        Boolean result = producerTemplate.requestBodyAndHeaders(
                "pqc:verify?operation=verify&signatureAlgorithm=Falcon&keyPair=#falconKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                headers,
                Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    // Sign operation using SPHINCSPlus
    @Path("/sign/sphincs")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincs() {
        byte[] signature = producerTemplate.requestBody(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPlus&keyPair=#sphincsKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    // Verify operation using SPHINCSPlus
    @Path("/verify/sphincs")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincs(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPqcSignature", signatureBytes);

        Boolean result = producerTemplate.requestBodyAndHeaders(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPlus&keyPair=#sphincsKeyPair",
                TEST_MESSAGE.getBytes(StandardCharsets.UTF_8),
                headers,
                Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    // KEM: generateSecretKeyEncapsulation operation using Kyber with AES
    @Path("/kem/encapsulate/kyber-aes")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String encapsulateKyberAes() {
        byte[] encapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=Kyber&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberKeyPair",
                null,
                byte[].class);
        return Base64.getEncoder().encodeToString(encapsulation);
    }

    // KEM: extractSecretKeyEncapsulation operation using Kyber with AES
    @Path("/kem/extract/kyber-aes")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberAes(String encapsulationBase64) {
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);

        byte[] secretKey = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=Kyber&symmetricKeyAlgorithm=AES&keyPair=#kyberKeyPair",
                encapsulation,
                byte[].class);
        return secretKey != null && secretKey.length > 0;
    }

    // KEM: extractSecretKeyFromEncapsulation operation using Kyber with AES (stores key in header)
    @Path("/kem/extract-to-header/kyber-aes")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberAesToHeader(String encapsulationBase64) {
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);

        Map<String, Object> headers = new HashMap<>();
        producerTemplate.requestBodyAndHeaders(
                "pqc:extractToHeader?operation=extractSecretKeyFromEncapsulation&keyEncapsulationAlgorithm=Kyber&symmetricKeyAlgorithm=AES&storeExtractedSecretKeyAsHeader=true&keyPair=#kyberKeyPair",
                encapsulation,
                headers);

        Object extractedKey = headers.get("CamelPqcExtractedSecretKey");
        return extractedKey != null;
    }

    // KEM with CHACHA7539
    @Path("/kem/encapsulate/kyber-chacha")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String encapsulateKyberChacha() {
        byte[] encapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=Kyber&symmetricKeyAlgorithm=CHACHA7539&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                null,
                byte[].class);
        return Base64.getEncoder().encodeToString(encapsulation);
    }

    @Path("/kem/extract/kyber-chacha")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberChacha(String encapsulationBase64) {
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);

        byte[] secretKey = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=Kyber&symmetricKeyAlgorithm=CHACHA7539&keyPair=#kyberKeyPair",
                encapsulation,
                byte[].class);
        return secretKey != null && secretKey.length > 0;
    }
}
