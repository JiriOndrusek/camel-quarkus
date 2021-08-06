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
package org.apache.camel.quarkus.component.aws2.ddb.deployment;

import java.util.Optional;

import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public class Aws2DdbProcessor {

    private static final String FEATURE = "camel-aws2-ddb";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    protected void setupExtension(BuildProducer<AmazonClientBuildItem> clientProducer,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase) {

        //Discover all clients injections in order to determine if async or sync client is required
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
            Type injectedType = getInjectedType(injectionPoint);

            if ("software.amazon.awssdk.services.dynamodb.DynamoDbClient".equals(injectedType.name())) {
                //registration is done via DynamoDbProcessor
                return;
            }
        }

        SyncHttpClientBuildTimeConfig buildTimeSyncConfig = new SyncHttpClientBuildTimeConfig();
        buildTimeSyncConfig.type = SyncHttpClientBuildTimeConfig.SyncClientType.APACHE;

        SdkBuildTimeConfig buildTimeSdkConfig = new SdkBuildTimeConfig();
        buildTimeSdkConfig.interceptors = Optional.empty();

        clientProducer.produce(new AmazonClientBuildItem(
                Optional.of(DotName.createSimple("software.amazon.awssdk.services.dynamodb.DynamoDbClient")),
                Optional.empty(),
                "dynamodb", //String from quarkus-amazon-dynamodb, has to be the same
                buildTimeSdkConfig,
                buildTimeSyncConfig));
    }

    private Type getInjectedType(InjectionPointInfo injectionPoint) {
        Type requiredType = injectionPoint.getRequiredType();
        Type injectedType = requiredType;

        if (DotNames.INSTANCE.equals(requiredType.name()) && requiredType instanceof ParameterizedType) {
            injectedType = requiredType.asParameterizedType().arguments().get(0);
        }

        return injectedType;
    }

}
