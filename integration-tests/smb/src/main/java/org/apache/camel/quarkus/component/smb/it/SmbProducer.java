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
package org.apache.camel.quarkus.component.smb.it;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smbj.SmbConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

public class SmbProducer {

    @Produces
    @Named("smbConfig")
    public SmbConfig SMBConfig() {

        System.out.println("******************* providers ********************");
        System.out.println(Arrays.stream(Security.getProviders()).map(Provider::getName).collect(Collectors.joining(", ")));

        //        Provider fipsProvider = Security.getProvider("SunPKCS11-NSS-FIPS");
        //
        //
        //        SecurityProvider runtimeSecurityProvider = new SecurityProvider() {
        //            @Override
        //            public MessageDigest getDigest(String name) throws SecurityException {
        //                return fipsProvider;
        //            }
        //
        //            @Override
        //            public Mac getMac(String name) throws SecurityException {
        //                return null;
        //            }
        //
        //            @Override
        //            public Cipher getCipher(String name) throws SecurityException {
        //                return null;
        //            }
        //
        //            @Override
        //            public AEADBlockCipher getAEADBlockCipher(String name) throws SecurityException {
        //                return null;
        //            }
        //
        //            @Override
        //            public DerivationFunction getDerivationFunction(String name) throws SecurityException {
        //                return null;
        //            }
        //        };

        //        Provider provider = Security.getProvider("SunPKCS11-NSS-FIPS");
        //            System.out.println(provider.getName());
        //            for (String key : provider.stringPropertyNames())
        //                System.out.println("\t" + key + "\t" + provider.getProperty(key));

        SmbConfig smbConfig = SmbConfig.builder(SmbConfig.createDefaultConfig())
                .withSecurityProvider(new JceSecurityProvider("SunPKCS11-NSS-FIPS")).build();
        return smbConfig;
    }

}
