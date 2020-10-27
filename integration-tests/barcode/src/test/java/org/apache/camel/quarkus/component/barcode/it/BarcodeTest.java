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
package org.apache.camel.quarkus.component.barcode.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class BarcodeTest {

    private static String MSG = "Hello camel-quarkus!";

    @Test
    public void testJpg() throws IOException {
        test("jpg", "JPEG");
    }

    @Test
    public void testPng() throws IOException {
        test("png", "png");
    }

    public void test(String type, String detectedType) throws IOException {
        byte[] bytes;
        try (InputStream is = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("type", type)
                .body(MSG)
                .post("/barcode/marshall")
                .then()
                .statusCode(201)
                .extract()
                .asInputStream()) {
            bytes = new byte[is.available()];
            is.read(bytes);
        }

        assertType(bytes, detectedType);

        RestAssured.given()
                .queryParam("type", type)
                .contentType(ContentType.BINARY)
                .body(bytes)
                .post("/barcode/unmarshall")
                .then()
                .statusCode(200)
                .body(is(MSG));
    }

    private void assertType(byte[] bytes, String expectedFormat) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        ImageReader reader = ImageIO.getImageReaders(iis).next();
        IOHelper.close(iis);

        String format = reader.getFormatName();
        assertEquals(expectedFormat, format);
    }
}
