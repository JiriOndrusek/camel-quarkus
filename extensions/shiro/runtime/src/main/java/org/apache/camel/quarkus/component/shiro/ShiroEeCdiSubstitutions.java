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
package org.apache.camel.quarkus.component.shiro;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.shiro.config.ogdl.DefaultInterpolator;
import org.apache.shiro.config.ogdl.Interpolator;
import org.apache.shiro.config.ogdl.ReflectionBuilder;

@TargetClass(value = ReflectionBuilder.class)
final class ReflectionBuilderSubstitution {

    @Substitute
    private Interpolator createInterpolator() {
        return new DefaultInterpolator();
    }
}

@TargetClass(className = "org.apache.shiro.ee.cdi.ShiroScopeContext", onlyWith = JakartaFacesAbsent.class)
@Delete
final class DeleteShiroScopeContext {
}

@TargetClass(className = "org.apache.shiro.ee.cdi.ShiroSessionScopeExtension", onlyWith = JakartaFacesAbsent.class)
@Delete
final class DeleteShiroSessionScopeExtension {
}

final class JakartaFacesAbsent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("jakarta.faces.view.ViewScoped");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
