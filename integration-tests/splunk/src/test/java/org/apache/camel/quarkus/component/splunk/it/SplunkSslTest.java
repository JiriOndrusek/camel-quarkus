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

import io.quarkus.test.junit.QuarkusTest;

//---------------------------
//probably can no works with free licence :
//<msg type="WARN">Remote login disabled because you are using a free license which does not provide authentication. To resolve either switch to the forwarder-only license or the enterprise trial license included with the product. To override this and enable unauthenticated remote management, edit the 'allowRemoteLogin' setting in your server.conf file.</msg>
//zkusit to same url bez ssh, co to udela
//--------------------
//https://docs.splunk.com/Documentation/Splunk/9.2.0/Admin/Serverconf
//https://docs.splunk.com/Documentation/Splunk/9.2.0/Security/ConfigTLSCertsS2S
//-----------------------

//The certificates must be in Privacy-Enhanced Mail format and comply with the x.509 public key certificate standard
//You must have a private key file for each certificate file.
//The key files that come with the certificates must be in RSA security format.
//https://docs.splunk.com/Documentation/Splunk/latest/Security/ConfigureSplunkforwardingtousesignedcertificates

//https://splunk.github.io/docker-splunk/ADVANCED.html#enable-ssl-internal-communication

//@TestCertificates(certificates = {
//        @Certificate(name = "splunk", formats = {
//                Format.PEM, Format.PKCS12 }, password = "password"),
//        @Certificate(name = "wrong", formats = {
//                Format.PKCS12 }, password = "password") })
@QuarkusTest
//@WithTestResource(value = SplunkTestResource.class, initArgs = {
//        @ResourceArg(name = "ssl", value = "true") })
class SplunkSslTest extends AbstractSplunkTest {

    SplunkSslTest() {
        super(true);
    }
}
