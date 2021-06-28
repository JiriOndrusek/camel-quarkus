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
package org.apache.camel.quarkus.component.avro.rpc.spi;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.camel.component.avro.spi.AvroRpcHttpServerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

public class VertxHttpServerFactory implements AvroRpcHttpServerFactory {
    @Override
    public Server create(SpecificResponder responder, int port) throws Exception {
        Vertx vertx = Arc.container().instance(Vertx.class).get();

        vertx.createHttpServer().requestHandler(getHttpServerRequestHandler()).listen(port);
    }

    private Handler<HttpServerRequest> getHttpServerRequestHandler() {
        return rc -> rc.response().end(" world!");
    }
}
