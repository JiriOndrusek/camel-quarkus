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
package org.apache.camel.quarkus.component.avro.rpc;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.arc.Arc;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.ResponderServlet;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;

@WebServlet
public class AvroRpcServlet extends HttpServlet {

    private Boolean reflect;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        if ("true".equals(config.getInitParameter("avro-rpc-specific")) &&
                "true".equals(config.getInitParameter("avro-rpc-reflect"))) {
            reflect = null;
        } else if ("true".equals(config.getInitParameter("avro-rpc-specific"))) {
            reflect = false;
        } else if ("true".equals(config.getInitParameter("avro-rpc-reflect"))) {
            reflect = true;
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        Registry registry = Arc.container().instance(CamelContext.class).get().getRegistry();

        Responder responder;
        if (reflect == null) {
            responder = registry.findByType(Responder.class).iterator().next();
        } else if (reflect) {
            responder = registry.findByTypeWithName(Responder.class).get("avro-rpc-reflect");
        } else {
            responder = registry.findByTypeWithName(Responder.class).get("avro-rpc-specific");
        }

        new ResponderServlet(responder).service(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
