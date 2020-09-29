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
package org.apache.camel.quarkus.component.shiro.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.shiro.security.ShiroSecurityPolicy;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;

public class ShiroRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        final ShiroSecurityPolicy securityPolicy = new ShiroSecurityPolicy(
                getClass().getResource("/securityConfig.ini").getPath(),
                ShiroResource.passPhrase);
        securityPolicy.setBase64(true);

        onException(UnknownAccountException.class, IncorrectCredentialsException.class,
                LockedAccountException.class, AuthenticationException.class).to("mock:authenticationException");

        from("direct:secureEndpoint").policy(securityPolicy).to("mock:success");
    }
}
