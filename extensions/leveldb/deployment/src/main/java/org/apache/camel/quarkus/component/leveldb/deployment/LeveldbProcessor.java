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
package org.apache.camel.quarkus.component.leveldb.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.support.DefaultExchangeHolder;

class LeveldbProcessor {

    private static final String FEATURE = "camel-leveldb";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(false, false, new String[] {
                org.iq80.leveldb.impl.Iq80DBFactory.class.getName(),
                org.apache.camel.support.DefaultExchangeHolder.class.getName()
        });
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection2() {
        return new ReflectiveClassBuildItem(true, true, new String[] {
                DefaultExchangeHolder.class.getName(),
        });
    }

    @BuildStep
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        resource.produce(new RuntimeInitializedClassBuildItem(
                org.iq80.leveldb.table.Table.class.getName()));
    }
    //
    //    @BuildStep
    //    public void registerRuntimeInitializedClasses2(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        resource.produce(new RuntimeInitializedClassBuildItem(
    //                MMapTable.class.getName()));
    //    }

    //    @BuildStep
    //    public void registerRuntimeInitializedClasses3(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        resource.produce(new RuntimeInitializedClassBuildItem(
    //                MMapLogWriter.class.getName()));
    //    }

    //    @BuildStep
    //    public void registerRuntimeInitializedClasses4(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        resource.produce(new RuntimeInitializedClassBuildItem(
    //                org.iq80.leveldb.table.MMapTable.Closer.class.getName()));
    //    }
    //
    //    @BuildStep
    //    public void registerRuntimeInitializedClasses5(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
    //        resource.produce(new RuntimeInitializedClassBuildItem(
    //                org.iq80.leveldb.util.ByteBufferSupport.class.getName()));
    //    }
}
