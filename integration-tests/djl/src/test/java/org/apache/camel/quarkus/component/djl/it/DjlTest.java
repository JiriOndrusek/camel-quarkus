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
package org.apache.camel.quarkus.component.djl.it;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DjlTest {

    //    @Test
    //    public void testExternalModel() throws Exception {
    //        post("data/mnist/0.png", external).body(is("0"));
    //        post("data/mnist/1.png", external).body(is("1"));
    //    }
    //
    //    //    @Test
    //    //    public void testLocalModel() throws Exception {
    //    //        post("data/mnist/0.png", local).body(is("0"));
    //    //        post("data/mnist/1.png", local).body(is("1"));
    //    //    }
    //
    //    private byte[] readFile(String fileName) throws Exception {
    //        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
    //            return readBytes(is);
    //        }
    //    }
    //
    //    private byte[] readBytes(InputStream is) throws Exception {
    //        ByteArrayOutputStream os = new ByteArrayOutputStream();
    //        byte[] buffer = new byte[4096];
    //        int len;
    //        while ((len = is.read(buffer)) != -1) {
    //            os.write(buffer, 0, len);
    //        }
    //        return os.toByteArray();
    //    }
    //
    //    private ValidatableResponse post(String fileName, DjlResource.ModelType modelType) throws Exception {
    //        return RestAssured.given()
    //                .contentType(ContentType.BINARY)
    //                .body(readFile(fileName))
    //                .post("djl/classificate/" + modelType)
    //                .then()
    //                .statusCode(200);
    //    }
}
