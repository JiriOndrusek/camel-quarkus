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

package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;

@ApplicationScoped
public class SpringRabbitmqRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("spring-rabbitmq:exchange-for-autoDeclare?queues=queue1-for-autoDeclare&routingKey=routing-key-for-autoDeclare&connectionFactory=#connectionFactory&autoDeclare=false")
                .transform(body().prepend("Hello from auto-declared1: "))
                .to("direct:autoDeclare1");

        from("spring-rabbitmq:exchange-for-autoDeclare?queues=queue2-for-autoDeclare&routingKey=routing-key-for-autoDeclare&connectionFactory=#connectionFactory&autoDeclare=true")
                .transform(body().prepend("Hello from auto-declared2: "))
                .to("direct:autoDeclare2");

        createRoute(SpringRabbitMQConstants.DIRECT_MESSAGE_LISTENER_CONTAINER);
        createRoute(SpringRabbitMQConstants.SIMPLE_MESSAGE_LISTENER_CONTAINER);
    }

    private void createRoute(String type) {
        String url = String.format(
                "spring-rabbitmq:%s?queues=%s&routingKey=%s&connectionFactory=#connectionFactory&autoDeclare=true&messageListenerContainerType=DMLC", //todo dmlc is hardcoded
                SpringRabbitmqResource.EXCHANGE_IN_OUT + type, type, SpringRabbitmqResource.ROUTING_KEY_IN_OUT + type);

        from(url)
                .transform(body().prepend("Hello "))
                .to(SpringRabbitmqResource.DIRECT_IN_OUT + type);
    }
}
