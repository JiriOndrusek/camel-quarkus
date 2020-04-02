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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class TikaTest {

    @Test
    public void testPdf() throws Exception {
        test("quarkus.pdf", "application/pdf", "Hello Quarkus");
    }

    @Test
    public void testOffice() throws Exception {
        String body = test("test.doc", "application/msword", "test");

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

    @Test
    public void testOdf() throws Exception {
        String body = test("testOpenOffice2.odt", "application/vnd.oasis.opendocument.text",
                "This is a sample Open Office document, written in NeoOffice 2.2.1 for the Mac");

        Charset detectedCharset = null;
        try {
            InputStream bodyIs = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_16));
            UniversalEncodingDetector encodingDetector = new UniversalEncodingDetector();
            detectedCharset = encodingDetector.detect(bodyIs, new Metadata());
        } catch (IOException e1) {
            Assertions.fail();
        }

        Assertions.assertTrue(detectedCharset.name().startsWith(StandardCharsets.UTF_16.name()));
    }
    //
    ////    @Test
    //    public void testGif() throws Exception {
    //        Path document = Paths.get("src/test/resources/testGIF.gif");
    //        test(document, "image/gif", null);
    //    }

    //---------------------------------------------------------------------------------------------------------

    private String test(String fileName, String expectedContentType, String expectedBody) throws Exception {
        String body = RestAssured.given() //
                .contentType(ContentType.BINARY)
                .body(readQuarkusFile(fileName))
                .post("/tika/post") //
                .then()
                .statusCode(201)
                .body(not(containsStringIgnoringCase("EmptyParser")))
                .body(containsStringIgnoringCase(expectedContentType))
                .body(containsStringIgnoringCase(expectedBody == null ? "<body/>" : expectedBody))
                .extract().asString();
        return body;
    }

    private byte[] readQuarkusFile(String fileName) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return readBytes(is);
        }
    }

    static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }
}
