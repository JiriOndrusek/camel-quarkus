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

import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.barcode.BarcodeDataFormat;
import org.apache.camel.dataformat.barcode.BarcodeImageType;
import org.apache.camel.spi.DataFormat;

public class BarcodeMarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testDataformats = new HashMap<>();

        testDataformats.put("jpg", new BarcodeDataFormat(200, 200, BarcodeImageType.JPG, BarcodeFormat.PDF_417));
        testDataformats.put("png", new BarcodeDataFormat(200, 200, BarcodeImageType.PNG, BarcodeFormat.AZTEC));
        testDataformats.put("gif", new BarcodeDataFormat(200, 200, BarcodeImageType.GIF, BarcodeFormat.CODABAR));

        for (Map.Entry<String, DataFormat> testDataformat : testDataformats.entrySet()) {
            from("direct:barcode-marshal-" + testDataformat.getKey()).marshal(testDataformat.getValue());
            from("direct:barcode-unmarshal-" + testDataformat.getKey()).unmarshal(testDataformat.getValue());
        }
    }

}
