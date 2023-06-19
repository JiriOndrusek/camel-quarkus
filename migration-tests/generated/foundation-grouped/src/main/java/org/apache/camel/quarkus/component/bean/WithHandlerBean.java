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
package org.apache.camel.quarkus.component.bean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;

@ApplicationScoped
@Named("withHandlerOnProxy")
@RegisterForReflection
public class WithHandlerBean {
    /**
     * Just set an hello message.
     */
    @Handler
    public void sayHello1(Exchange exchange) {
        String body = exchange.getMessage().getBody(String.class);
        exchange.getMessage().setBody("Hello " + body + " from the WithHandlerBean");
    }

    /**
     * Just set an hello message.
     */
    public void sayHello2(Exchange exchange) {
        throw new IllegalStateException("This method should not be invoked");
    }
}
