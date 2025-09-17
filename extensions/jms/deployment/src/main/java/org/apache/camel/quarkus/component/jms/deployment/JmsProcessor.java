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
package org.apache.camel.quarkus.component.jms.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.quarkus.component.jms.CamelJmsRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.apache.camel.support.ClassicUuidGenerator;

class JmsProcessor {

    private static final String FEATURE = "camel-jms";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CamelSerializationBuildItem serialization() {
        return new CamelSerializationBuildItem();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void customizer(BuildProducer<CamelContextCustomizerBuildItem> customizers, CamelJmsRecorder recorder) {
        try {
            Class.forName("org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactoryImpl");
            customizers.produce(new CamelContextCustomizerBuildItem(recorder.createCamelJmsCustomizer()));
        } catch (ClassNotFoundException e) {
            // Only create the JMS component customizer if the ActiveMQ Artemis RA is available
        }
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ClassicUuidGenerator.class.getName()));
        //todo quick fix, jgroups should not be mentioned here, but the quarkus-artemis is on org.jgroups:jgroups:jar:5.3.13.Final
        // and Camel (and CQ) uses 5.4.11.Final; when a newer quarkus-artemis is released with the newer jgroups, this workaround can be removed
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("org.jgroups.util.Util"));
    }
}
