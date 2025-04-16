package org.apache.camel.quarkus.component.jasypt.it;

import org.apache.camel.quarkus.test.FipsModeUtil;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;

public class JasyptEncodingHelper {


    public static void main(String[] args) {
        String msg = "Hello World has to be much longer to work on FIPS system!";

//        System.out.println("encoded messages is: '%s'".formatted(encode()));;
        if(FipsModeUtil.isFipsMode()) {
            System.out.println("FIPS encoded messages is: '%s'".formatted(
                    encode(msg, "2s3cr3t", "PBEWithHMACSHA256AndAES_256", "PKCS11", "PKCS11")
            ));
        }
    }


    private static String encode(String message, String password, String algorithm, String saltAlgorithm, String ivGeneratorAlgorithm) {
        StandardPBEStringEncryptor pbeStringEncryptor = new StandardPBEStringEncryptor();

        pbeStringEncryptor.setPassword(password);
        pbeStringEncryptor.setAlgorithm(algorithm);

        if (saltAlgorithm != null) {
            pbeStringEncryptor.setSaltGenerator(new RandomSaltGenerator(saltAlgorithm));
        }
        if (ivGeneratorAlgorithm != null) {
            pbeStringEncryptor.setIvGenerator(new RandomIvGenerator(ivGeneratorAlgorithm));
        }

        return pbeStringEncryptor.encrypt(message);
    }
}
