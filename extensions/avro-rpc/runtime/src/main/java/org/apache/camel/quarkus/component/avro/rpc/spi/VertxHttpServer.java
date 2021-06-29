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
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;

public class VertxHttpServer implements Server {

    private int port;

    private Responder responder;

    public VertxHttpServer(Responder servletAvro, int port) {
        this.port = port;
        this.responder = servletAvro;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        Vertx vertx = Arc.container().instance(Vertx.class).get();

        vertx.createHttpServer().requestHandler(rc -> {
            rc.bodyHandler(handler -> {
                HttpServletRequestFromBytes req = new HttpServletRequestFromBytes(handler.getBytes());
                HttpServletResponseWithBytesStream resp = new HttpServletResponseWithBytesStream();

                try {
                    new ResponderServlet(responder).service(req, resp);
                    Buffer buff = Buffer.buffer(resp.getBytes());
                    rc.response().end(buff);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }).listen(port);
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public void join() throws InterruptedException {
        throw new AvroRuntimeException(new UnsupportedOperationException());
    }

}
