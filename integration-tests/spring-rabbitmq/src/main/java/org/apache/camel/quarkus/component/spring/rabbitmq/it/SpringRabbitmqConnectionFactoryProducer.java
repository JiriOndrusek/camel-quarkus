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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

public class SpringRabbitmqConnectionFactoryProducer {

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_PORT)
    Integer port;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_HOSTNAME)
    String hostname;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_USERNAME)
    String usernane;

    @ConfigProperty(name = SpringRabbitmqResource.PARAMETER_PASSWORD)
    String password;

    @Produces
    @ApplicationScoped
    @Named("connectionFactory")
    public ConnectionFactory produceMinioClient() {
        CachingConnectionFactory cf = new CachingConnectionFactory();
        cf.setUri(String.format("amqp://%s:%d", hostname, port));
        cf.setUsername(usernane);
        cf.setPassword(password);

//        Queue q = new Queue("myqueue");
//        TopicExchange t = new TopicExchange("foo");
//
//        AmqpAdmin admin = new RabbitAdmin(cf);
//        admin.declareQueue(q);
//        admin.declareExchange(t);
//        admin.declareBinding(BindingBuilder.bind(q).to(t).with("foo.bar.#"));

        return cf;
    }
}
