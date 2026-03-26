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
    private KeyPair dilithium3Key;
    private KeyPair dilithium5Key;
    private KeyPair falconKey;
    private KeyPair falcon1024Key;
    private KeyPair sphincsKey;
    private KeyPair sphincsSha2_128sKey;
    private KeyPair sphincsSha2_192fKey;
    private KeyPair sphincsSha2_256fKey;
    private KeyPair sphincsShake_128fKey;
    private KeyPair sphincsShake_256sKey;
    private KeyPair kyberKey;
    private KeyPair kyber768Key;
    private KeyPair kyber1024Key;

    // Additional keypairs for negative tests (wrong key scenarios)
    private KeyPair dilithiumWrongKey;
    private KeyPair kyberWrongKey;

    @Inject
    ProducerTemplate producerTemplate;

    @PostConstruct
    public void init() throws Exception {
        //        Security.addProvider(new BouncyCastlePQCProvider());

        // Generate keypairs once
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        dilithiumKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium3, new SecureRandom());
        dilithium3Key = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium5, new SecureRandom());
        dilithium5Key = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_512, new SecureRandom());
        falconKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_1024, new SecureRandom());
        falcon1024Key = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
        sphincsKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128s, new SecureRandom());
        sphincsSha2_128sKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_192f, new SecureRandom());
        sphincsSha2_192fKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_256f, new SecureRandom());
        sphincsSha2_256fKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.shake_128f, new SecureRandom());
        sphincsShake_128fKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.shake_256s, new SecureRandom());
        sphincsShake_256sKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        kyberKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber768, new SecureRandom());
        kyber768Key = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber1024, new SecureRandom());
        kyber1024Key = gen.generateKeyPair();

        // Generate additional keypairs for negative tests (wrong key scenarios)
        gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        dilithiumWrongKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        kyberWrongKey = gen.generateKeyPair();
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

    @Produces
    @Named("dilithium3KeyPair")
    public KeyPair dilithium3KeyPair() {
        return dilithium3Key;
    }

    @Produces
    @Named("dilithium5KeyPair")
    public KeyPair dilithium5KeyPair() {
        return dilithium5Key;
    }

    @Produces
    @Named("falcon1024KeyPair")
    public KeyPair falcon1024KeyPair() {
        return falcon1024Key;
    }

    @Produces
    @Named("sphincsSha2_128sKeyPair")
    public KeyPair sphincsSha2_128sKeyPair() {
        return sphincsSha2_128sKey;
    }

    @Produces
    @Named("sphincsSha2_192fKeyPair")
    public KeyPair sphincsSha2_192fKeyPair() {
        return sphincsSha2_192fKey;
    }

    @Produces
    @Named("sphincsSha2_256fKeyPair")
    public KeyPair sphincsSha2_256fKeyPair() {
        return sphincsSha2_256fKey;
    }

    @Produces
    @Named("sphincsShake_128fKeyPair")
    public KeyPair sphincsShake_128fKeyPair() {
        return sphincsShake_128fKey;
    }

    @Produces
    @Named("sphincsShake_256sKeyPair")
    public KeyPair sphincsShake_256sKeyPair() {
        return sphincsShake_256sKey;
    }

    @Produces
    @Named("kyber768KeyPair")
    public KeyPair kyber768KeyPair() {
        return kyber768Key;
    }

    @Produces
    @Named("kyber1024KeyPair")
    public KeyPair kyber1024KeyPair() {
        return kyber1024Key;
    }

    @Produces
    @Named("dilithiumWrongKeyPair")
    public KeyPair dilithiumWrongKeyPair() {
        return dilithiumWrongKey;
    }

    @Produces
    @Named("kyberWrongKeyPair")
    public KeyPair kyberWrongKeyPair() {
        return kyberWrongKey;
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

    // Negative test: verify with corrupted signature
    @Path("/verify/dilithium/corrupt")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithCorruptedSignature(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        // Corrupt the signature by flipping a byte
        if (signatureBytes.length > 10) {
            signatureBytes[10] = (byte) ~signatureBytes[10];
        }

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

    // Negative test: verify with wrong keypair
    @Path("/verify/dilithium/wrongkey")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithWrongKeyPair(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumWrongKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Negative test: verify with algorithm mismatch (sign with Dilithium, verify with Falcon)
    @Path("/verify/falcon/mismatch")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String verifyWithAlgorithmMismatch(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        try {
            Exchange exchange = producerTemplate.request(
                    "pqc:verify?operation=verify&signatureAlgorithm=FALCON&keyPair=#falconKeyPair",
                    ex -> {
                        ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                        ex.getIn().setHeaders(headers);
                    });

            Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
            return Boolean.TRUE.equals(verification) ? "true" : "false";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Negative test: sign with null body
    @Path("/sign/dilithium/null")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithNullBody() {
        try {
            Exchange exchange = producerTemplate.request(
                    "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                    ex -> ex.getIn().setBody(null));

            byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
            return signature != null ? "success" : "null";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Negative test: verify without signature header
    @Path("/verify/dilithium/nosignature")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String verifyWithoutSignatureHeader() {
        try {
            Exchange exchange = producerTemplate.request(
                    "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                    ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

            Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
            return verification != null ? verification.toString() : "null";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Negative test: KEM extract with wrong keypair
    @Path("/kem/extract/kyber-aes/wrongkey")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String extractWithWrongKey(String encapsulationBase64) {
        if (kyberAesEncapsulation == null) {
            return "no-encapsulation";
        }

        try {
            SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                    "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberWrongKeyPair",
                    kyberAesEncapsulation,
                    SecretKeyWithEncapsulation.class);

            byte[] originalKey = kyberAesEncapsulation.getEncoded();
            byte[] extractedKey = result != null ? result.getEncoded() : null;

            if (extractedKey != null && originalKey != null) {
                return java.util.Arrays.equals(originalKey, extractedKey) ? "same" : "different";
            }
            return "null";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Parameter spec tests: Dilithium3
    @Path("/sign/dilithium3")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithDilithium3() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithium3KeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/dilithium3")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithDilithium3(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithium3KeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Parameter spec tests: Dilithium5
    @Path("/sign/dilithium5")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithDilithium5() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithium5KeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/dilithium5")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithDilithium5(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithium5KeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Parameter spec tests: Falcon 1024
    @Path("/sign/falcon1024")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithFalcon1024() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=FALCON&keyPair=#falcon1024KeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/falcon1024")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithFalcon1024(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=FALCON&keyPair=#falcon1024KeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Parameter spec tests: SPHINCS+ variants
    @Path("/sign/sphincs/sha2_128s")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincsSha2_128s() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_128sKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/sphincs/sha2_128s")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincsSha2_128s(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_128sKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/sign/sphincs/sha2_192f")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincsSha2_192f() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_192fKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/sphincs/sha2_192f")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincsSha2_192f(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_192fKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/sign/sphincs/sha2_256f")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincsSha2_256f() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_256fKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/sphincs/sha2_256f")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincsSha2_256f(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsSha2_256fKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/sign/sphincs/shake_128f")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincsShake_128f() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsShake_128fKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/sphincs/shake_128f")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincsShake_128f(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsShake_128fKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/sign/sphincs/shake_256s")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithSphincsShake_256s() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsShake_256sKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return Base64.getEncoder().encodeToString(signature);
    }

    @Path("/verify/sphincs/shake_256s")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithSphincsShake_256s(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=SPHINCSPLUS&keyPair=#sphincsShake_256sKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Parameter spec tests: Kyber variants
    @Path("/kem/kyber768")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean testKyber768() {
        SecretKeyWithEncapsulation encap = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyber768KeyPair",
                null,
                SecretKeyWithEncapsulation.class);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyber768KeyPair",
                encap,
                SecretKeyWithEncapsulation.class);

        return result != null && result.getEncoded().length > 0;
    }

    @Path("/kem/kyber1024")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean testKyber1024() {
        SecretKeyWithEncapsulation encap = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyber1024KeyPair",
                null,
                SecretKeyWithEncapsulation.class);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyber1024KeyPair",
                encap,
                SecretKeyWithEncapsulation.class);

        return result != null && result.getEncoded().length > 0;
    }

    // Symmetric algorithm tests: AES-256
    @Path("/kem/aes256")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean testAes256() {
        SecretKeyWithEncapsulation encap = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                null,
                SecretKeyWithEncapsulation.class);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                encap,
                SecretKeyWithEncapsulation.class);

        return result != null && result.getEncoded().length > 0;
    }

    // Symmetric algorithm tests: CHACHA-256 (Note: CHACHA7539 only supports 256-bit keys)
    @Path("/kem/chacha256")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean testChacha256() {
        SecretKeyWithEncapsulation encap = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=CHACHA7539&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                null,
                SecretKeyWithEncapsulation.class);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=CHACHA7539&symmetricKeyLength=256&keyPair=#kyberKeyPair",
                encap,
                SecretKeyWithEncapsulation.class);

        return result != null && result.getEncoded().length > 0;
    }

    // Body tests: empty body
    @Path("/sign/emptybody")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithEmptyBody() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(new byte[0]));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return signature != null ? Base64.getEncoder().encodeToString(signature) : "null";
    }

    @Path("/verify/emptybody")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithEmptyBody(String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> {
                    ex.getIn().setBody(new byte[0]);
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Body tests: large body
    @Path("/sign/largebody/{size}")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithLargeBody(@jakarta.ws.rs.PathParam("size") int size) {
        byte[] largeData = new byte[size];
        new SecureRandom().nextBytes(largeData);

        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(largeData));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return signature != null ? Base64.getEncoder().encodeToString(signature) : "null";
    }

    @Path("/verify/largebody/{size}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithLargeBody(@jakarta.ws.rs.PathParam("size") int size, String signatureBase64) {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        byte[] largeData = new byte[size];
        new SecureRandom().nextBytes(largeData);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", signatureBytes);

        Exchange exchange = producerTemplate.request(
                "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> {
                    ex.getIn().setBody(largeData);
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    // Body tests: binary data
    @Path("/sign/binarydata")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithBinaryData() {
        byte[] binaryData = new byte[1024];
        new SecureRandom().nextBytes(binaryData);

        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(binaryData));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        // Store binary data for verification
        exchange.getIn().setHeader("CamelTestBinaryData", binaryData);
        return signature != null ? Base64.getEncoder().encodeToString(signature) + ":" +
                Base64.getEncoder().encodeToString(binaryData) : "null";
    }

    @Path("/verify/binarydata")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean verifyWithBinaryData(String combined) {
        String[] parts = combined.split(":");
        byte[] signatureBytes = Base64.getDecoder().decode(parts[0]);
        byte[] binaryData = Base64.getDecoder().decode(parts[1]);

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

    // Body preservation test
    @Path("/sign/preservebody")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signAndCheckBodyPreservation(String message) {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(message.getBytes(StandardCharsets.UTF_8)));

        byte[] bodyAfter = exchange.getMessage().getBody(byte[].class);
        String bodyString = new String(bodyAfter, StandardCharsets.UTF_8);
        return message.equals(bodyString) ? "preserved" : "changed";
    }

    // Header tests: header preservation
    @Path("/sign/headerpreservation")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String signWithCustomHeader() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CustomHeader", "CustomValue");

        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> {
                    ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object customHeader = exchange.getMessage().getHeader("CustomHeader");
        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);

        return "CustomValue".equals(customHeader) && signature != null ? "preserved" : "lost";
    }

    // Header tests: malformed signature
    @Path("/verify/malformedheader")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String verifyWithMalformedSignatureHeader() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelPQCSignature", "not-valid-bytes");

        try {
            Exchange exchange = producerTemplate.request(
                    "pqc:verify?operation=verify&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                    ex -> {
                        ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
                        ex.getIn().setHeaders(headers);
                    });

            Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
            return verification != null ? verification.toString() : "null";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Header tests: extract without store to header
    @Path("/kem/extract-no-header")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String extractWithoutStoreToHeader() {
        if (kyberAesEncapsulation == null) {
            return "no-encapsulation";
        }

        Exchange exchange = producerTemplate.request(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&storeExtractedSecretKeyAsHeader=false&keyPair=#kyberKeyPair",
                ex -> ex.getIn().setBody(kyberAesEncapsulation));

        Object headerKey = exchange.getMessage().getHeader("CamelPQCSecretKey");
        byte[] bodyKey = exchange.getMessage().getBody(byte[].class);

        return (headerKey == null && bodyKey != null && bodyKey.length > 0) ? "in-body" : "in-header";
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

    // Native mode test: check algorithms
    @Path("/algorithms/check")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String checkAlgorithms() {
        try {
            KeyPairGenerator.getInstance("Dilithium", "BCPQC");
            KeyPairGenerator.getInstance("Falcon", "BCPQC");
            KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
            KeyPairGenerator.getInstance("Kyber", "BCPQC");
            return "all-available";
        } catch (Exception e) {
            return "error:" + e.getClass().getSimpleName();
        }
    }

    // Concurrency test endpoints
    @Path("/concurrent/sign")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String concurrentSign() {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=DILITHIUM&keyPair=#dilithiumKeyPair",
                ex -> ex.getIn().setBody(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = exchange.getMessage().getHeader("CamelPQCSignature", byte[].class);
        return signature != null ? Base64.getEncoder().encodeToString(signature) : "null";
    }

    @Path("/concurrent/kem")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean concurrentKem() {
        SecretKeyWithEncapsulation encap = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberKeyPair",
                null,
                SecretKeyWithEncapsulation.class);

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=KYBER&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=#kyberKeyPair",
                encap,
                SecretKeyWithEncapsulation.class);

        return result != null && result.getEncoded().length > 0;
    }
}
