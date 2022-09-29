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
package org.apache.camel.quarkus.component.quartz;

import javax.enterprise.inject.Instance;

import com.oracle.svm.core.annotate.Inject;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.quartz.QuartzComponent;
import org.quartz.Scheduler;

@Recorder
public class CamelQuartzRecorder {

    public RuntimeValue<QuartzComponent> createQuartzComponent(BeanContainer beanContainer) {
        //        QuartzSchedulerImpl impl = beanContainer.instanceFactory(QuartzSchedulerImpl.class).create().get();
        //
        //        QuartzSchedulerImpl quartzScheduler = beanContainer.instance(QuartzSchedulerImpl.class);
        QuartzComponent component = new QuarkusQuartzComponent();
        //        component.setAutowiredEnabled(false);
        //        component.setScheduler(impl.getScheduler());
        //        component.setScheduler(quartzScheduler.getScheduler());

        return new RuntimeValue<>(component);
    }

    @org.apache.camel.spi.annotations.Component("quartz")
    static class QuarkusQuartzComponent extends QuartzComponent {

        @Inject
        Instance<Scheduler> quartzScheduler;

        public QuarkusQuartzComponent() {
            System.out.println("*******************************************************");
            System.out.println("*******************************************************");
        }
    }
}
