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
package org.apache.camel.quarkus.test.support.pqc.certificate.validation;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;

import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.jboss.logging.Logger;

/**
 * TrustManager that validates hybrid RSA+PQC certificates following BC Almanac recommendations.
 * https://downloads.bouncycastle.org/java/docs/PQC-Almanac.pdf
 *
 * Validates both:
 * 1. Primary RSA signature (standard X.509 validation)
 * 2. Alternative ML-DSA signature from Extension.altSignatureValue using key from Extension.subjectAltPublicKeyInfo
 *
 * Note: Uses NIST-standardized ML-DSA (Module-Lattice-Based Digital Signature Algorithm) names
 * as defined in FIPS 204. ML-DSA-44 is the standardized name for Dilithium2.
 */
public class HybridCertificateTrustManager implements X509TrustManager {

    private static final Logger LOG = Logger.getLogger(HybridCertificateTrustManager.class);
    private static final String BCPQC_PROVIDER = "BC"; // Using BC provider for NIST-standardized ML-DSA algorithms
    private static final String BC_PROVIDER = "BC";

    static {
        // Ensure BC provider is registered
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        // BCPQC provider kept for backward compatibility but not required for ML-DSA
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Not used in this test scenario
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Certificate chain is empty");
        }

        X509Certificate cert = chain[0];

        try {
            // Step 1: Validate primary RSA signature (standard X.509 validation)
            cert.checkValidity();
            cert.verify(cert.getPublicKey()); // Self-signed for test purposes
            LOG.info("Primary RSA signature validated successfully");

            // Step 2: Extract and validate alternative PQC signature
            byte[] altPublicKeyBytes = cert.getExtensionValue(Extension.subjectAltPublicKeyInfo.getId());
            byte[] altSignatureBytes = cert.getExtensionValue(Extension.altSignatureValue.getId());

            if (altPublicKeyBytes == null || altSignatureBytes == null) {
                LOG.warn("Certificate does not contain PQC extensions - treating as standard RSA certificate");
                return;
            }

            // Parse the alternative public key
            // Extension values are wrapped in OCTET STRING, need to unwrap first
            ASN1OctetString altPubKeyOctet = ASN1OctetString.getInstance(altPublicKeyBytes);
            SubjectPublicKeyInfo altPubKeyInfo;
            try (ASN1InputStream ais = new ASN1InputStream(altPubKeyOctet.getOctets())) {
                altPubKeyInfo = SubjectPublicKeyInfo.getInstance(ais.readObject());
            }

            // Convert SubjectPublicKeyInfo to PublicKey using KeyFactory
            // This works for both legacy Dilithium and NIST ML-DSA formats
            KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-44", BC_PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(altPubKeyInfo.getEncoded());
            PublicKey dilithiumPublicKey = keyFactory.generatePublic(keySpec);

            // Parse the alternative signature
            ASN1OctetString altSigOctet = ASN1OctetString.getInstance(altSignatureBytes);
            ASN1BitString altSigBitString;
            try (ASN1InputStream ais = new ASN1InputStream(altSigOctet.getOctets())) {
                altSigBitString = ASN1BitString.getInstance(ais.readObject());
            }
            byte[] dilithiumSignature = altSigBitString.getBytes();

            // Verify the ML-DSA signature using NIST-standardized algorithm name
            Signature dilithiumVerifier = Signature.getInstance("ML-DSA-44", BCPQC_PROVIDER);
            dilithiumVerifier.initVerify(dilithiumPublicKey);
            dilithiumVerifier.update(cert.getSubjectX500Principal().getEncoded());

            if (!dilithiumVerifier.verify(dilithiumSignature)) {
                throw new CertificateException("Alternative ML-DSA signature validation failed");
            }

            LOG.info("Alternative ML-DSA signature validated successfully - hybrid certificate verified");

        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Failed to validate hybrid certificate: " + e.getMessage(), e);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
