package org.apache.camel.quarkus.component.avro.rpc.it;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;

import javax.servlet.ServletException;
import java.io.IOException;

public class HttpQuarkusServerOld implements Server {

    private Undertow server;

    private int port;

    public HttpQuarkusServerOld(Responder servlet, int port) throws IOException {
        this.port = port;
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new HttpQuarkusServletHandler(servlet))
                .build();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new AvroRuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new AvroRuntimeException(e);
        }
    }

    @Override
    public void join() throws InterruptedException {
        throw new AvroRuntimeException(new UnsupportedOperationException());
    }

    private class HttpQuarkusServletHandler implements HttpHandler {

        private final ResponderServlet responderServlet;

        public HttpQuarkusServletHandler(final Responder responder) throws IOException {
            this.responderServlet = new ResponderServlet(responder);
        }

        @Override
        public void handleRequest(final HttpServerExchange exchange) throws IOException, ServletException {

//            final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
//            ServletRequest request = servletRequestContext.getServletRequest();
//            ServletResponse response = servletRequestContext.getServletResponse();
//            ServletRequest request = ((VertxHttpExchange) exchange.getDelegate()).getreqrequest
//            InstanceHandle<? extends Servlet> servlet = null;


            try {
                responderServlet.service(new HttpServletRequestImpl(exchange, null), new HttpServletResponseImpl(exchange, null));
            } finally {
//                if (servlet != null) {
//                    servlet.release();
//                }
            }
        }
    }
}
