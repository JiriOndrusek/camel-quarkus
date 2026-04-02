Analyse for me, how to move the code 
''
    @Produces
    @Singleton
    @Named("dilithiumKeyPair")
    public KeyPair dilithiumKeyPair() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("Dilithium2", "BCPQC");

        byte[] publicKeyBytes = Files.readAllBytes(
                Paths.get("target/certs/dilithium-public.key"));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        byte[] privateKeyBytes = Files.readAllBytes(
                Paths.get("target/certs/dilithium-private.key"));
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        return new KeyPair(publicKey, privateKey);
    }
'''
from integration-test-groups/http/http/... HttpProducers to be generic and part of the https://github.com/smallrye/smallrye-certificate-generator/tree/main
