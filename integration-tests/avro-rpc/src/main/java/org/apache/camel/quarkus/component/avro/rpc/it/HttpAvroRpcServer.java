package org.apache.camel.quarkus.component.avro.rpc.it;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.avro.ipc.Server;

public class HttpAvroRpcServer implements Server {

    private Undertow server;

    private int port;

    public HttpAvroRpcServer(Responder servletAvro, int port) throws IOException, ServletException {

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .setContextPath("/*")
                .setDeploymentName("avro-rpc-http")
                .addServletContextAttribute("avro-servlet-param", servletAvro)
                .addServlets(
                        Servlets.servlet("Avro-rpc servlet", AvroRpcServlet.class)
                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        HttpHandler servletHandler = manager.start();

        this.port = port;
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new PathHandler(servletHandler))
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
            responderServlet.service(new HttpServletRequestImpl(exchange, null), new HttpServletResponseImpl(exchange, null));
        }
    }

}


