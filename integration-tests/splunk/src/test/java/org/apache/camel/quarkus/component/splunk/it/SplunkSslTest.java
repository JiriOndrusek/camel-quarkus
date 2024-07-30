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
package org.apache.camel.quarkus.component.splunk.it;

import java.util.concurrent.ExecutionException;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.test.support.splunk.SplunkTestResource;
import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
//@WithTestResource(value = SplunkFakeResource.class)
@WithTestResource(value = SplunkTestResource.class, initArgs = {
        @ResourceArg(name = "ssl", value = "true"), @ResourceArg(name = "localhost_pem", value = "keytool/combined.pem"),
        @ResourceArg(name = "ca_pem", value = "keytool/splunkca.pem") })
class SplunkSslTest extends AbstractSplunkTest {

    @Test
    public void testNormalSearchWithSubmitWithRawData() throws InterruptedException {
        super.testNormalSearchWithSubmitWithRawData(true);
    }

    @Test
    public void testSavedSearchWithTcp() throws InterruptedException {
        super.testSavedSearchWithTcp(true);
    }

    @Test
    public void testStreamForRealtime() throws InterruptedException, ExecutionException {
        super.testStreamForRealtime(true);
    }

    @Test
    public void testNormalSearchWithSubmitWithRawDataNoSSL() throws InterruptedException {
        //creation of data has to be executed with ssl
        super.testNormalSearchWithSubmitWithRawData(true, false, true);
    }

    @Test
    public void testSavedSearchWithTcpNoSSL() throws InterruptedException {
        Assertions.assertThrowsExactly(NoHttpResponseException.class,
                () -> super.testSavedSearchWithTcp(false));
    }

    @Test
    public void testStreamForRealtimeNoSSL() throws InterruptedException, ExecutionException {
        super.testStreamForRealtime(true, false, true);
    }
}
