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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.pqc.crypto.lms.LMOtsParameters;
import org.bouncycastle.pqc.crypto.lms.LMSigParameters;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.FalconParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.LMSParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.SPHINCSPlusParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.XMSSParameterSpec;
import org.jboss.logging.Logger;

@Path("/pqc")
@ApplicationScoped
public class PqcResource {

    private static final Logger LOG = Logger.getLogger(PqcResource.class);;

    private Map<String, SecretKeyWithEncapsulation> secretKeys = new HashMap<>();

    // Store encapsulation results in memory for testing
    private SecretKeyWithEncapsulation kyberAesEncapsulation;
    private SecretKeyWithEncapsulation kyberChachaEncapsulation;

    // Store keypairs as fields (generated once in @PostConstruct)
    private KeyPair dilithiumKey;
    private KeyPair falconKey;
    private KeyPair sphincsKey;
    private KeyPair lmsKey;
    private KeyPair xmssKey;
    private KeyPair kyberKey;

    // Additional keypair for negative tests (wrong key scenario)
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

        gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_512, new SecureRandom());
        falconKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
        sphincsKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("LMS", "BCPQC");
        gen.initialize(new LMSParameterSpec(LMSigParameters.lms_sha256_n32_h5, LMOtsParameters.sha256_n32_w4),
                new SecureRandom());
        lmsKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("XMSS", "BCPQC");
        gen.initialize(XMSSParameterSpec.SHA2_10_256, new SecureRandom());
        xmssKey = gen.generateKeyPair();

        gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        kyberKey = gen.generateKeyPair();

        // Generate additional keypair for negative test (wrong key scenario)
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
    @Named("lmsKeyPair")
    public KeyPair lmsKeyPair() {
        return lmsKey;
    }

    @Produces
    @Named("xmssKeyPair")
    public KeyPair xmssKeyPair() {
        return xmssKey;
    }

    @Produces
    @Named("kyberKeyPair")
    public KeyPair kyberKeyPair() {
        return kyberKey;
    }

    @Produces
    @Named("kyberWrongKeyPair")
    public KeyPair kyberWrongKeyPair() {
        return kyberWrongKey;
    }

    @Path("/sign/{algorithm}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String sign(String message, @PathParam("algorithm") String algorithm) {
        Exchange exchange = producerTemplate.request(
                "pqc:sign?operation=sign&signatureAlgorithm=%s&keyPair=#%s".formatted(algorithm, toKeyPair(algorithm)),
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
                "pqc:verify?operation=verify&signatureAlgorithm=%s&keyPair=#%s".formatted(algorithm, toKeyPair(algorithm)),
                ex -> {
                    ex.getIn().setBody(message.getBytes(StandardCharsets.UTF_8));
                    ex.getIn().setHeaders(headers);
                });

        Object verification = exchange.getMessage().getHeader("CamelPQCVerification");
        return Boolean.TRUE.equals(verification);
    }

    @Path("/kem/encapsulate/{algorithm}")
    @POST
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public String encapsulate(@PathParam("algorithm") String algorithm) {
        SecretKeyWithEncapsulation secretKeyWithEncapsulation = producerTemplate.requestBody(
                "pqc:encapsulate?operation=generateSecretKeyEncapsulation&keyEncapsulationAlgorithm=%s&symmetricKeyAlgorithm=AES&symmetricKeyLength=128&keyPair=%s"
                        .formatted(algorithm, toKeyPair(algorithm)),
                null,
                SecretKeyWithEncapsulation.class);
        String enc = Base64.getEncoder().encodeToString(secretKeyWithEncapsulation.getEncapsulation());
        secretKeys.put(enc, secretKeyWithEncapsulation);
        return enc;
    }

    @Path("/kem/extract/{algorithm}/{keyAlgorithm}/{length}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public boolean extract(String encapsulationBase64,
            @PathParam("algorithm") String algorithm,
            @PathParam("keyAlgorithm") String keyAlgorithm,
            @PathParam("length") int length) {

        SecretKeyWithEncapsulation result = producerTemplate.requestBody(
                "pqc:extract?operation=extractSecretKeyEncapsulation&keyEncapsulationAlgorithm=%s&symmetricKeyAlgorithm=%s&symmetricKeyLength=%s&keyPair=%s"
                        .formatted(algorithm, keyAlgorithm, length, toKeyPair(algorithm)),
                secretKeys.get(encapsulationBase64),
                SecretKeyWithEncapsulation.class);
        byte[] secretKey = result != null ? result.getEncoded() : null;
        return secretKey != null && secretKey.length > 0;
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
