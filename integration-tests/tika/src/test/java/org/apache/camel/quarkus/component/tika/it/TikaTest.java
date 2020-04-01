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
package org.apache.camel.quarkus.component.tika.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
class TikaTest {

    @Test
    public void testDoc() throws Exception {
        Path document = Paths.get("src/test/resources/test.doc");
        String body = RestAssured.given() //
                .contentType(ContentType.BINARY)
                .body(Files.readAllBytes(document))
                .post("/tika/post") //
                .then()
                .statusCode(201)
                .body(containsStringIgnoringCase("application/msword"))
                .body(containsStringIgnoringCase("test"))
                .extract().asString();

        Charset detectedCharset = null;
        try {
            InputStream bodyIs = new ByteArrayInputStream(body.getBytes());
            UniversalEncodingDetector encodingDetector = new UniversalEncodingDetector();
            detectedCharset = encodingDetector.detect(bodyIs, new Metadata());
        } catch (IOException e1) {
            Assertions.fail();
        }

        Assertions.assertTrue(detectedCharset.name().startsWith(Charset.defaultCharset().name()));
    }

}
