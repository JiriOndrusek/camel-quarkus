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
package org.apache.camel.quarkus.component.jasypt.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class JasyptFipsModeTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.camel.jasypt.algorithm", "PBEWithHMACSHA256AndAES_256",
                "quarkus.camel.jasypt.random-iv-generator-algorithm", "PKCS11",
                "quarkus.camel.jasypt.random-salt-generator-algorithm", "PKCS11",
                "greeting.secret", "ENC(xzuMOkyUMYG8mM4qnFXKf3HxngLky9EJTeP3vvNLp2QeLaESJL77NDD00kWBQLdrlLP2WjOjx5t8Nky1G9ez5mrr3Iiqkn7x/FGGdxXX/Wga0i289k+0ThrggjpkB5D0)",
                "explicit.config.provider.secret", "ENC(xzuMOkyUMYG8mM4qnFXKf3HxngLky9EJTeP3vvNLp2QeLaESJL77NDD00kWBQLdrlLP2WjOjx5t8Nky1G9ez5mrr3Iiqkn7x/FGGdxXX/Wga0i289k+0ThrggjpkB5D0)",
                "camel.component.direct.timeout", "30000",
                "timer.delay.secret", "ENC(xzuMOkyUMYG8mM4qnFXKf3HxngLky9EJTeP3vvNLp2QeLaESJL77NDD00kWBQLdrlLP2WjOjx5t8Nky1G9ez5mrr3Iiqkn7x/FGGdxXX/Wga0i289k+0ThrggjpkB5D0)",
                "timer.repeatCount.secret", "ENC(xzuMOkyUMYG8mM4qnFXKf3HxngLky9EJTeP3vvNLp2QeLaESJL77NDD00kWBQLdrlLP2WjOjx5t8Nky1G9ez5mrr3Iiqkn7x/FGGdxXX/Wga0i289k+0ThrggjpkB5D0)",
                "%custom-profile.greeting.expression.secret", "ENC(xzuMOkyUMYG8mM4qnFXKf3HxngLky9EJTeP3vvNLp2QeLaESJL77NDD00kWBQLdrlLP2WjOjx5t8Nky1G9ez5mrr3Iiqkn7x/FGGdxXX/Wga0i289k+0ThrggjpkB5D0)");
    }
}
