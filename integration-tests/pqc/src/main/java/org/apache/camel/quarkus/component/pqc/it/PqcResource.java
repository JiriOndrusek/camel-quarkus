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
import java.security.Signature;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.jboss.logging.Logger;

@Path("/pqc")
@ApplicationScoped
public class PqcResource {

    private static final Logger LOG = Logger.getLogger(PqcResource.class);
    private static final String COMPONENT_PQC = "pqc";
    private static final String TEST_MESSAGE = "Hello Camel Quarkus PQC";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    private KeyPair mlDsaKeyPair;
    private KeyPair mlKemKeyPair;

    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastlePQCProvider());

        // Generate ML-DSA (Dilithium) key pair for signatures
        KeyPairGenerator mlDsaGen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        mlDsaGen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        mlDsaKeyPair = mlDsaGen.generateKeyPair();

        // Generate ML-KEM (Kyber) key pair for key encapsulation
        KeyPairGenerator mlKemGen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        mlKemGen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        mlKemKeyPair = mlKemGen.generateKeyPair();
    }

    @Path("/load/component/pqc")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponentPqc() throws Exception {
        if (context.getComponent(COMPONENT_PQC) != null) {
            return Response.ok().build();
        }
        LOG.warnf("Could not load [%s] from the Camel context", COMPONENT_PQC);
        return Response.status(500, COMPONENT_PQC + " could not be loaded from the Camel context").build();
    }

    @Path("/mldsa/sign")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String mlDsaSign() throws Exception {
        Signature signature = Signature.getInstance("Dilithium", "BCPQC");
        signature.initSign(mlDsaKeyPair.getPrivate());
        signature.update(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    @Path("/mldsa/verify")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean mlDsaVerify(String signatureBase64) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        Signature signature = Signature.getInstance("Dilithium", "BCPQC");
        signature.initVerify(mlDsaKeyPair.getPublic());
        signature.update(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
        return signature.verify(signatureBytes);
    }

    @Path("/mlkem/encapsulate")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String mlKemEncapsulate() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("Kyber", "BCPQC");
        keyGen.init(new KEMGenerateSpec(mlKemKeyPair.getPublic(), "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secEnc = (SecretKeyWithEncapsulation) keyGen.generateKey();
        byte[] encapsulation = secEnc.getEncapsulation();
        return Base64.getEncoder().encodeToString(encapsulation);
    }

    @Path("/mlkem/extract")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean mlKemExtract(String encapsulationBase64) throws Exception {
        byte[] encapsulation = Base64.getDecoder().decode(encapsulationBase64);
        KeyGenerator keyGen = KeyGenerator.getInstance("Kyber", "BCPQC");
        keyGen.init(new KEMExtractSpec(mlKemKeyPair.getPrivate(), encapsulation, "AES"), new SecureRandom());
        SecretKey secKey = keyGen.generateKey();
        return secKey != null && secKey.getEncoded() != null;
    }
}
