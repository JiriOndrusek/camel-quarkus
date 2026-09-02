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
package org.apache.camel.quarkus.component.weaviate.minimal.it;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessResult;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "quarkus.runner", matches = ".*runner.jar", disabledReason = "https://github.com/apache/camel-quarkus/issues/4218")
public class WeaviateMinimalTest {

    @Test
    void applicationStarts() throws InvalidExitValueException, IOException, InterruptedException, TimeoutException {
        final ProcessResult result = new QuarkusProcessExecutor("-Dcamel.main.duration-max-seconds=1").execute();

        assertThat(result.getExitValue()).isEqualTo(0);
        assertThat(result.outputUTF8()).contains("Apache Camel");
        assertThat(result.outputUTF8()).contains("started");
    }
}
