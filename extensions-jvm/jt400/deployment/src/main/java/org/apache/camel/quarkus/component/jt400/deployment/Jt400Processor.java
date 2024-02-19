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
package org.apache.camel.quarkus.component.jt400.deployment;

import java.util.ArrayList;
import java.util.List;

import com.ibm.as400.access.AS400ImplRemote;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.logging.Logger;

class Jt400Processor {

    private static final Logger LOG = Logger.getLogger(Jt400Processor.class);
    private static final String FEATURE = "camel-jt400";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<ReflectiveClassBuildItem>();
        items.add(ReflectiveClassBuildItem.builder(AS400ImplRemote.class).build());
        items.add(ReflectiveClassBuildItem.builder("com.ibm.as400.access.AS400ImplProxy").build());
        return items;
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        List<RuntimeInitializedClassBuildItem> items = new ArrayList<>();
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.CredentialVault"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.GSSTokenVault"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.IdentityTokenVault"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.PasswordVault"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.ProfileTokenVault"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.AS400"));
        items.add(new RuntimeInitializedClassBuildItem("java.lang.Thread"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.PasswordDialog"));
        //        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.ToolboxSignonHandler"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.MessageDialog"));
        items.add(new RuntimeInitializedClassBuildItem("sun.java2d.Disposer"));
        items.add(new RuntimeInitializedClassBuildItem("org.apache.camel.component.jt400.Jt400PgmProducer"));
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.AS400"));
        return items;
    }

    //    NativeImageSystemPropertyBuildItem nativeSystemProperty() {
    //        return new NativeImageSystemPropertyBuildItem("com.ibm.as400.access.AS400.guiAvailable", "false");
    //    }
}
