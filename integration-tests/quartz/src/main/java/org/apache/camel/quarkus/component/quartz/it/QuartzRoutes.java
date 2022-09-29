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
package org.apache.camel.quarkus.component.quartz.it;

import org.apache.camel.builder.RouteBuilder;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuartzRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("quartz:quartz/1 * * * * ?")
                .setBody(constant("Hello Camel Quarkus quartz"))
                .to("seda:quartz-result");

        from("cron:tab?schedule=0/1 * * * * ?")
                .setBody(constant("Hello Camel Quarkus cron"))
                .to("seda:cron-result");

        from("quartzFromProperties:properties/* 1 * * * ")
                .setBody(constant("Hello Camel Quarkus Quartz Properties"))
                .to("seda:quartz-properties-result");

        // cron trigger
        from("quartz://cronTrigger?cron=0/1+*+*+*+*+?&trigger.timeZone=Europe/Stockholm")
                .setBody(constant("Hello Camel Quarkus Quartz From Cron Trigger"))
                .to("seda:quartz-cron-trigger-result");

        from("quartz://misfire?cron=0/1+*+*+*+*+?&trigger.timeZone=Europe/Stockholm&trigger.misfireInstruction=2")
                .to("seda:quartz-cron-misfire-result");
    }

    public Scheduler create() {
        return new Scheduler() {
            @Override
            public String getSchedulerName() throws SchedulerException {
                return null;
            }

            @Override
            public String getSchedulerInstanceId() throws SchedulerException {
                return null;
            }

            @Override
            public SchedulerContext getContext() throws SchedulerException {
                return null;
            }

            @Override
            public void start() throws SchedulerException {

            }

            @Override
            public void startDelayed(int seconds) throws SchedulerException {

            }

            @Override
            public boolean isStarted() throws SchedulerException {
                return false;
            }

            @Override
            public void standby() throws SchedulerException {

            }

            @Override
            public boolean isInStandbyMode() throws SchedulerException {
                return false;
            }

            @Override
            public void shutdown() throws SchedulerException {

            }

            @Override
            public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {

            }

            @Override
            public boolean isShutdown() throws SchedulerException {
                return false;
            }

            @Override
            public SchedulerMetaData getMetaData() throws SchedulerException {
                return null;
            }

            @Override
            public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
                return null;
            }

            @Override
            public void setJobFactory(JobFactory factory) throws SchedulerException {

            }

            @Override
            public ListenerManager getListenerManager() throws SchedulerException {
                return null;
            }

            @Override
            public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
                return null;
            }

            @Override
            public Date scheduleJob(Trigger trigger) throws SchedulerException {
                return null;
            }

            @Override
            public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {

            }

            @Override
            public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException {

            }

            @Override
            public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
                return false;
            }

            @Override
            public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
                return false;
            }

            @Override
            public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
                return null;
            }

            @Override
            public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {

            }

            @Override
            public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException {

            }

            @Override
            public boolean deleteJob(JobKey jobKey) throws SchedulerException {
                return false;
            }

            @Override
            public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
                return false;
            }

            @Override
            public void triggerJob(JobKey jobKey) throws SchedulerException {

            }

            @Override
            public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {

            }

            @Override
            public void pauseJob(JobKey jobKey) throws SchedulerException {

            }

            @Override
            public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {

            }

            @Override
            public void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {

            }

            @Override
            public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {

            }

            @Override
            public void resumeJob(JobKey jobKey) throws SchedulerException {

            }

            @Override
            public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {

            }

            @Override
            public void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {

            }

            @Override
            public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {

            }

            @Override
            public void pauseAll() throws SchedulerException {

            }

            @Override
            public void resumeAll() throws SchedulerException {

            }

            @Override
            public List<String> getJobGroupNames() throws SchedulerException {
                return null;
            }

            @Override
            public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
                return null;
            }

            @Override
            public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
                return null;
            }

            @Override
            public List<String> getTriggerGroupNames() throws SchedulerException {
                return null;
            }

            @Override
            public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
                return null;
            }

            @Override
            public Set<String> getPausedTriggerGroups() throws SchedulerException {
                return null;
            }

            @Override
            public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
                return null;
            }

            @Override
            public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
                return null;
            }

            @Override
            public Trigger.TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
                return null;
            }

            @Override
            public void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException {

            }

            @Override
            public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException {

            }

            @Override
            public boolean deleteCalendar(String calName) throws SchedulerException {
                return false;
            }

            @Override
            public Calendar getCalendar(String calName) throws SchedulerException {
                return null;
            }

            @Override
            public List<String> getCalendarNames() throws SchedulerException {
                return null;
            }

            @Override
            public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException {
                return false;
            }

            @Override
            public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
                return false;
            }

            @Override
            public boolean checkExists(JobKey jobKey) throws SchedulerException {
                return false;
            }

            @Override
            public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
                return false;
            }

            @Override
            public void clear() throws SchedulerException {

            }
        };
    }
}
