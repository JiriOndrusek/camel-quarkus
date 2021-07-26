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
package org.apache.camel.quarkus.main.unknown.args.fail;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class MainUnknownArgumentFailTest {

    //@Test
    public void testMainTerminatesOnUnknownArguments() throws InterruptedException, IOException, TimeoutException {
        final ProcessResult result = new QuarkusProcessExecutor(new String[] {}, "-d", "10", "-cp", "foo.jar", "-t").execute();

        // Verify the application did not run successfully
        assertThat(result.getExitValue()).isEqualTo(1);
        assertThat(result.outputUTF8()).doesNotContain("Timer tick!");

        // Verify warning for unknown arguments was printed to the console
        assertThat(result.outputUTF8()).contains("Unknown option: -cp foo.jar");
        assertThat(result.outputUTF8()).contains("Apache Camel Runner takes the following options");
    }
}
