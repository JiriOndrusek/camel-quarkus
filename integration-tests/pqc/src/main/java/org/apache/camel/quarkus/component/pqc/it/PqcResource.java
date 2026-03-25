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
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
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

    // Store encapsulation results in memory for testing
    private SecretKeyWithEncapsulation kyberAesEncapsulation;
    private SecretKeyWithEncapsulation kyberChachaEncapsulation;

    // Store keypairs as fields (generated once in @PostConstruct)
    private KeyPair dilithiumKey;
    private KeyPair falconKey;
    private KeyPair sphincsKey;
    private KeyPair kyberKey;

    @Inject
    ProducerTemplate producerTemplate;

    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastlePQCProvider());

        // Generate keypairs once
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        dilithiumKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_512, new SecureRandom());
        falconKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
        sphincsKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        kyberKey = gen.generateKeyPair();
    }

    // Register KeyPairs as CDI beans for Camel registry
    @Produces
    @Named("dilithiumKeyPair")
    public KeyPair dilithiumKeyPair() {
        return dilithiumKey;
    }

    @Produces
    @Named("falconKeyPair")
    public KeyPair falconKeyPair() {
        return falconKey;
    }

    @Produces
    @Named("sphincsKeyPair")
    public KeyPair sphincsKeyPair() {
        return sphincsKey;
    }

    @Produces
    @Named("kyberKeyPair")
    public KeyPair kyberKeyPair() {
        return kyberKey;
    }

    // Sign operation using ML-DSA (Dilithium)
    @Path("/sign/dilithium")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithDilithium() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        // The sign operation outputs signature in the HEADER, not the body
        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
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
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Sign operation using Falcon
    @Path("/sign/falcon")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithFalcon() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=FALCON&keyPair=#falconKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        // The sign operation outputs signature in the HEADER, not the body
        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
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
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=FALCON&keyPair=#falconKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Sign operation using SPHINCSPlus
    @Path("/sign/sphincs")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincs() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        // The sign operation outputs signature in the HEADER, not the body
        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
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
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // KEM: generateSecretKeyEncapsulation operation using Kyber with AES
    @Path("/kem/encapsulate/kyber-aes")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String encapsulateKyberAes() {
        kyberAesEncapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberKeyPair",
                null,
                SecretKeyWithEncapsulation.class);
        return Base64.getEncoder().encodeToString(kyberAesEncapsulation.getEncapsulation());
    }

    // KEM: extractSecretKeyEncapsulation operation using Kyber with AES
    @Path("/kem/extract/kyber-aes")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberAes(String encapsulationBase64) {
        // Use the stored encapsulation object
        if (kyberAesEncapsulation == null) {
            return false;
        }

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberKeyPair",
                kyberAesEncapsulation,
                SecretKeyWithEncapsulation.class);
        byte[] secretKey = result != null ? result.getEncoded() : null;
        return secretKey != null && secretKey.length > 0;
    }

    // KEM: extractSecretKeyFromEncapsulation operation using Kyber with AES (stores key in header)
    @Path("/kem/extract-to-header/kyber-aes")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberAesToHeader(String encapsulationBase64) {
        // Use the stored encapsulation object
        if (kyberAesEncapsulation == null) {
            return false;
        }

        Exchange exchange = producerTemplate.request(
                "pqc:extractToHeader?operation=extractSecretKeyFromEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&storeExtractedSecretKeyAsHeader=true&keyPair=#kyberKeyPair",
                ex -> ex.getIn().setBody(kyberAesEncapsulation));

        Object extractedKey = exchange.getMessage().getHeader("CamelPQCSecretKey");
        return extractedKey != null;
    }

    // KEM with CHACHA7539
    @Path("/kem/encapsulate/kyber-chacha")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String encapsulateKyberChacha() {
        kyberChachaEncapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=CHACHA7539&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                null,
                SecretKeyWithEncapsulation.class);
        return Base64.getEncoder().encodeToString(kyberChachaEncapsulation.getEncapsulation());
    }

    @Path("/kem/extract/kyber-chacha")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extractKyberChacha(String encapsulationBase64) {
        // Use the stored encapsulation object
        if (kyberChachaEncapsulation == null) {
            return false;
        }

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=CHACHA7539&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                kyberChachaEncapsulation,
                SecretKeyWithEncapsulation.class);
        byte[] secretKey = result != null ? result.getEncoded() : null;
        return secretKey != null && secretKey.length > 0;
    }
}
