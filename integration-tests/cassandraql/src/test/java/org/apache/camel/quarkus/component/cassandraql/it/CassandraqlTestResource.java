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

package org.apache.camel.quarkus.component.cassandraql.it;

public class CassandraqlTestResource { /*implements ContainerResourceLifecycleManager {
                                       private static final Logger LOGGER = LoggerFactory.getLogger(CassandraqlTestResource.class);
                                       
                                       protected GenericContainer container;
                                       
                                       @Override
                                       public Map<String, String> start() {
                                       LOGGER.info(TestcontainersConfiguration.getInstance().toString());
                                       try {
                                       CassandraContainer cc = new CassandraContainer().withInitScript("init_script.cql");
                                       container = cc.withExposedPorts(9042);
                                       
                                       container.start();
                                       
                                       return CollectionHelper.mapOf(
                                       CassandraqlResource.DB_URL_PARAMETER,
                                       container.getContainerIpAddress() + ":" + container.getMappedPort(9042));
                                       
                                       } catch (Exception e) {
                                       LOGGER.error("Container does not start", e);
                                       throw new RuntimeException(e);
                                       }
                                       }
                                       
                                       @Override
                                       public void stop() {
                                       try {
                                       if (container != null) {
                                       container.stop();
                                       }
                                       } catch (Exception e) {
                                       // ignored
                                       }
                                       }*/
}
