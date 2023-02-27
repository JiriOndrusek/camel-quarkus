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
package org.apache.camel.quarkus.component.jdbc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class JdbcRoutes extends RouteBuilder {

    @Inject
    TransactionManager transactionManager;

    @Override
    public void configure() throws Exception {

        from("direct:xa")
                .transacted("PROPAGATION_REQUIRED")
                .process(x -> {
                    transactionManager.getTransaction().enlistResource(new DummyXAResource());
                })
                .to("direct:insert")
                .choice()
                .when(body().convertToString().contains("fail"))
                .log("Forced to rollback")
                .process(x -> {
                    transactionManager.setRollbackOnly();
                })
                .endChoice();

        from("direct:insert")
                .setBody(simple("insert into camels (id, species) values (${header.id}, 'Camelus ${body}')"))
                .to("jdbc:camel-ds?resetAutoCommit=false");
    }
}
