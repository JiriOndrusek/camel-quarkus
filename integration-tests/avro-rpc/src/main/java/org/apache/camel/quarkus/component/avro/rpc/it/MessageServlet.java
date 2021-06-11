package org.apache.camel.quarkus.component.avro.rpc.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RegisterForReflection
public class MessageServlet extends HttpServlet {

    public static final String MESSAGE = "message";

    private String message;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        message = config.getInitParameter(MESSAGE);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        Responder responder = (Responder) req.getServletContext().getAttribute("avro");

        new ResponderServlet(responder).service(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        Responder responder = (Responder) req.getServletContext().getAttribute("avro");
//responder.respond()
        new ResponderServlet(responder).service(req, resp);

    }
}