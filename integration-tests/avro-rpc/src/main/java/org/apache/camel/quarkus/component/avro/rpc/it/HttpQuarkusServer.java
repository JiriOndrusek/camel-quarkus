package org.apache.camel.quarkus.component.avro.rpc.it;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.vertx.VertxHttpExchange;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;

public class HttpQuarkusServer implements Server {

    private Undertow server;

    private int port;

    public HttpQuarkusServer(Responder servletAvro, int port) throws IOException, ServletException {

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .setContextPath("/*")
                .setDeploymentName("avro")
                .addServletContextAttribute("avro", servletAvro)
                .addServlets(
                        Servlets.servlet("MessageServlet", MessageServlet.class)
//                                .addInitParam("message", "Hello World")
                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        HttpHandler servletHandler = manager.start();
        PathHandler path = Handlers.path(Handlers.redirect("/*"))
                .addPrefixPath("/*", servletHandler);

//        HttpHandler servletHandler = manager.start();

        this.port = port;
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(path)
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
