package org.apache.camel.quarkus.component.pqc.it;

import java.security.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bouncycastle.pqc.crypto.lms.LMOtsParameters;
import org.bouncycastle.pqc.crypto.lms.LMSigParameters;
import org.bouncycastle.pqc.jcajce.spec.*;

@ApplicationScoped
public class PqcProducers {

    @Produces
    @Singleton
    @Named("dilithiumKeyPair")
    KeyPair dillithiumKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
        gen.initialize(DilithiumParameterSpec.dilithium2, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("falconKeyPair")
    public KeyPair falconKeyPair()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Falcon", "BCPQC");
        gen.initialize(FalconParameterSpec.falcon_512, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("sphincsKeyPair")
    public KeyPair sphincsKeyPair()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("SPHINCSPlus", "BCPQC");
        gen.initialize(SPHINCSPlusParameterSpec.sha2_128f, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("lmsKeyPair")
    public KeyPair lmsKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("LMS", "BCPQC");
        gen.initialize(new LMSParameterSpec(LMSigParameters.lms_sha256_n32_h5, LMOtsParameters.sha256_n32_w4),
                new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("xmssKeyPair")
    public KeyPair xmssKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("XMSS", "BCPQC");
        gen.initialize(XMSSParameterSpec.SHA2_10_256, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("kyberKeyPair")
    public KeyPair kyberKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        return gen.generateKeyPair();
    }

    @Produces
    @Singleton
    @Named("kyberWrongKeyPair")
    public KeyPair kyberWrongKeyPair()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        gen.initialize(KyberParameterSpec.kyber512, new SecureRandom());
        return gen.generateKeyPair();
    }
}
