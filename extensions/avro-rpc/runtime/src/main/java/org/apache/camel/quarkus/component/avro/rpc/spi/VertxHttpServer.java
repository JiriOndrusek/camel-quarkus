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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.arc.Arc;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
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
            try {
                rc.body(handle(responder, rc));
                //new ResponderServlet(responder).service(null, rc.response());
                System.out.println("-------------------------request");
                //                rc.uploadHandler(httpServerFileUpload -> System.out.println(httpServerFileUpload.size()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

    private Handler<AsyncResult<io.vertx.core.buffer.Buffer>> handle(Responder responder, HttpServerRequest rc) {
        return new Handler<AsyncResult<Buffer>>() {
            @Override
            public void handle(AsyncResult<Buffer> bufferAsyncResult) {
                Buffer b = bufferAsyncResult.result();

                try {
                    List<ByteBuffer> in = readBuffers(new ByteArrayInputStream(b.getBytes()));
                    List<ByteBuffer> result = responder.respond(in);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writeBuffers(result, baos);
                    Buffer buff = Buffer.buffer(baos.toByteArray());
                    rc.response().end(buff);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //                responder.respond(Collections.singletonList(b));
                System.out.println("-giot result");
            }
        };
    }

    static List<ByteBuffer> readBuffers(InputStream in) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<>();
        while (true) {
            int length = (in.read() << 24) + (in.read() << 16) + (in.read() << 8) + in.read();
            if (length == 0) { // end of buffers
                return buffers;
            }
            ByteBuffer buffer = ByteBuffer.allocate(length);
            while (buffer.hasRemaining()) {
                int p = buffer.position();
                int i = in.read(buffer.array(), p, buffer.remaining());
                if (i < 0)
                    throw new EOFException("Unexpected EOF");
                ((java.nio.Buffer) buffer).position(p + i);
            }
            ((java.nio.Buffer) buffer).flip();
            buffers.add(buffer);
        }
    }

    static void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
        for (ByteBuffer buffer : buffers) {
            writeLength(buffer.limit(), out); // length-prefix
            out.write(buffer.array(), buffer.position(), buffer.remaining());
            ((java.nio.Buffer) buffer).position(buffer.limit());
        }
        writeLength(0, out); // null-terminate
    }

    private static void writeLength(int length, OutputStream out) throws IOException {
        out.write(0xff & (length >>> 24));
        out.write(0xff & (length >>> 16));
        out.write(0xff & (length >>> 8));
        out.write(0xff & length);
    }

}
