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
package org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.cxf.way.it;

import java.io.IOException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Map;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.xml.ws.BindingProvider;
import org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.it.PasswordCallbackHandler;
import org.apache.cxf.ws.security.SecurityConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CxfWssSecurityPolicyServerCxfWayTest {

    static {
        SecureRandom sr = new java.security.SecureRandom();
        System.out.println("****************************************************");
        System.out.println("****************************************************");
        for (Provider provider: Security.getProviders()) {
            System.out.println(provider.getName());
//            for (String key: provider.stringPropertyNames())
//                System.out.println("\t" + key + "\t" + provider.getProperty(key));
        }
        System.out.println("****************************************************");
        System.out.println("****************************************************");

    }

    @Test
    void encrypetdSigned() throws IOException {
        WssSecurityPolicyHelloServiceCxfWay client = getPlainClient();

        Map<String, Object> ctx = ((BindingProvider) client).getRequestContext();
        ctx.put(SecurityConstants.CALLBACK_HANDLER, new PasswordCallbackHandler());
        ctx.put(SecurityConstants.SIGNATURE_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("alice.properties"));
        ctx.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        ctx.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        ctx.put(SecurityConstants.ENCRYPT_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("alice.properties"));

        Assertions.assertThat(client.sayHello("foo")).isEqualTo("SecurityPolicy hello foo CXF way");
    }

    WssSecurityPolicyHelloServiceCxfWay getPlainClient() {
        return QuarkusCxfClientTestUtil.getClient(
                "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy",
                WssSecurityPolicyHelloServiceCxfWay.class,
                "/soapservice/security-policy-hello-cxf-way");
    }
}
