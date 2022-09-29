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

import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.enterprise.inject.AmbiguousResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.quartz.runtime.QuartzSchedulerImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.quartz.QuartzComponent;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

@Recorder
public class CamelQuartzRecorder {

    public RuntimeValue<QuartzComponent> createQuartzComponent(BeanContainer beanContainer) {
        return new RuntimeValue<>(new QuarkusQuartzComponent());
    }

    @org.apache.camel.spi.annotations.Component("quartz")
    static class QuarkusQuartzComponent extends QuartzComponent {

        private boolean autowireExecuted;

        @Override
        public boolean isAutowiredEnabled() {
            if (autowireExecuted) {
                return false;
            }

            if (super.isAutowiredEnabled()) {
                InjectableInstance<Scheduler> schedulers = Arc.container().select(Scheduler.class);

                LinkedList<Scheduler> foundSchedulers = new LinkedList<>();

                for (InstanceHandle<Scheduler> handle : schedulers.handles()) {
                    if (handle.getBean().getBeanClass().equals(QuartzSchedulerImpl.class)) {
                        Scheduler scheduler = Arc.container().select(QuartzScheduler.class).getHandle().get().getScheduler();
                        if (scheduler != null) {
                            foundSchedulers.add(scheduler);
                        }
                        continue;
                    }
                    foundSchedulers.add(handle.get());
                }

                if (foundSchedulers.size() > 1) {
                    throw new AmbiguousResolutionException(String.format("Found %d org.quartz.Scheduler beans (%s).",
                            foundSchedulers.size(), foundSchedulers.stream().map(s -> {
                                try {
                                    return s.getSchedulerName();
                                } catch (SchedulerException e) {
                                    return "Name retrieval failed.";
                                }
                            }).collect(Collectors.joining(", "))));
                } else if (!foundSchedulers.isEmpty()) {
                    setScheduler(foundSchedulers.getFirst());
                }
            }

            autowireExecuted = true;
            return false;
        }
    }
}
