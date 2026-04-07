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
package org.apache.camel.quarkus.test.support.pqc.certificate;

import java.nio.file.Path;

public class PqcCertificatesUtil {
    public static final Path DEFAULT_CERTS_BASEDIR = Path.of("target/certs");

    private PqcCertificatesUtil() {
    }

    public static Path certPem(String name) {
        return file(name + "-cert", "pem");
    }

    private static Path file(String name, String extension) {
        return DEFAULT_CERTS_BASEDIR.resolve(name + "." + extension);
    }
}
