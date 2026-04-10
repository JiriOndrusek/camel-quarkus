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
package org.apache.camel.opentelemetry2;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.telemetry.Op;
import org.apache.camel.telemetry.Span;
import org.apache.camel.telemetry.SpanContextPropagationExtractor;
import org.apache.camel.telemetry.SpanContextPropagationInjector;
import org.apache.camel.telemetry.SpanLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link OpenTelemetryTracer} to properly set SpanKind based on the operation type (send vs receive).
 * This works around the limitation in the upstream Apache Camel telemetry API where SpanLifecycleManager.create()
 * doesn't accept a SpanKind parameter.
 */
public class CamelQuarkusOpenTelemetry2Tracer extends OpenTelemetryTracer {

    private static final Logger LOG = LoggerFactory.getLogger(CamelQuarkusOpenTelemetry2Tracer.class);

    // ThreadLocal to pass Op context from beginEventSpan to SpanLifecycleManager
    private static final ThreadLocal<String> CURRENT_OP = new ThreadLocal<>();

    // ThreadLocal to pass endpoint URI to SpanLifecycleManager for special handling
    private static final ThreadLocal<String> CURRENT_ENDPOINT_URI = new ThreadLocal<>();

    @Override
    protected void initTracer() {
        // Initialize the tracer and context propagators as normal
        super.initTracer();

        // Get the tracer and context propagators from the parent initialization
        Tracer tracer = CamelContextHelper.findSingleByType(getCamelContext(), Tracer.class);
        if (tracer == null) {
            tracer = GlobalOpenTelemetry.get().getTracer("camel");
        }
        if (tracer == null) {
            throw new RuntimeCamelException("Could not find any Opentelemetry tracer!");
        }

        ContextPropagators contextPropagators = CamelContextHelper.findSingleByType(
                getCamelContext(), ContextPropagators.class);
        if (contextPropagators == null) {
            contextPropagators = GlobalOpenTelemetry.get().getPropagators();
        }
        if (contextPropagators == null) {
            throw new RuntimeCamelException("Could not find any Opentelemetry context propagator!");
        }

        // Install our custom SpanLifecycleManager that sets SpanKind
        this.setSpanLifecycleManager(new QuarkusOpenTelemetrySpanLifecycleManager(tracer, contextPropagators));
    }

    @Override
    protected void beginEventSpan(Exchange exchange, Endpoint endpoint, Op op) throws Exception {
        // Store the operation type and endpoint URI in ThreadLocal so the SpanLifecycleManager can determine the appropriate SpanKind
        CURRENT_OP.set(op.toString());
        CURRENT_ENDPOINT_URI.set(endpoint.getEndpointUri());
        try {
            super.beginEventSpan(exchange, endpoint, op);
        } finally {
            // Clean up ThreadLocal to avoid leaks
            CURRENT_OP.remove();
            CURRENT_ENDPOINT_URI.remove();
        }
    }

    @Override
    protected void beginProcessorSpan(Exchange exchange, String processorName) throws Exception {
        // Processor spans are always INTERNAL
        CURRENT_OP.set("EVENT_PROCESS");
        try {
            super.beginProcessorSpan(exchange, processorName);
        } finally {
            CURRENT_OP.remove();
        }
    }

    /**
     * Custom SpanLifecycleManager that sets the appropriate SpanKind based on the operation type.
     */
    private static class QuarkusOpenTelemetrySpanLifecycleManager implements SpanLifecycleManager {

        private final Tracer tracer;
        private final ContextPropagators contextPropagators;

        private QuarkusOpenTelemetrySpanLifecycleManager(Tracer tracer, ContextPropagators contextPropagators) {
            this.tracer = tracer;
            this.contextPropagators = contextPropagators;
        }

        @Override
        public Span create(String spanName, Span parent, SpanContextPropagationExtractor extractor) {
            SpanBuilder builder = tracer.spanBuilder(spanName);
            Baggage baggage = null;

            // Determine the appropriate SpanKind based on the operation type
            SpanKind spanKind = determineSpanKind();
            if (spanKind != null) {
                builder.setSpanKind(spanKind);
            }

            if (parent != null) {
                OpenTelemetrySpanAdapter otelParentSpan = (OpenTelemetrySpanAdapter) parent;
                builder = builder.setParent(Context.current().with(otelParentSpan.getSpan()));
                baggage = otelParentSpan.getBaggage();
            } else {
                Context current = Context.root();
                // If the current span was generated by Camel, then, this is a "dirty" context.
                // A "dirty" context happens when the Camel thread local is reused and
                // due to the way Camel async works, can't reliably clean its context before reusing it.
                if (Baggage.current().getEntryValue(OpenTelemetrySpanAdapter.BAGGAGE_CAMEL_FLAG) == null) {
                    // Not "dirty" context. In this case a Span exists and the current span was generated by some third party dependency (ie, vertx)
                    // therefore we need to consider this span as the root on such a trace.
                    current = Context.current();
                }
                // Try to get parent from context propagation (upstream traces)
                Context ctx = contextPropagators.getTextMapPropagator().extract(current, extractor,
                        new TextMapGetter<SpanContextPropagationExtractor>() {
                            @Override
                            public Iterable<String> keys(SpanContextPropagationExtractor carrier) {
                                return carrier.keys();
                            }

                            @Override
                            public String get(SpanContextPropagationExtractor carrier, String key) {
                                if (carrier.get(key) == null) {
                                    return null;
                                }
                                return carrier.get(key).toString();
                            }
                        });

                builder = builder.setParent(ctx);
                baggage = Baggage.fromContext(ctx);
            }

            return new OpenTelemetrySpanAdapter(builder.startSpan(), baggage);
        }

        /**
         * Determine the SpanKind based on the operation type stored in ThreadLocal.
         */
        private SpanKind determineSpanKind() {
            String op = CURRENT_OP.get();
            if (op == null) {
                return SpanKind.INTERNAL;
            }

            String endpointUri = CURRENT_ENDPOINT_URI.get();

            return switch (op) {
            case "EVENT_SENT" -> {
                // Sending to an endpoint should be CLIENT for messaging/routing components
                // (direct, seda, vm, etc.) but INTERNAL for HTTP/transport components that
                // create their own client spans
                if (endpointUri != null && isMessagingEndpoint(endpointUri)) {
                    yield SpanKind.CLIENT;
                }
                yield SpanKind.INTERNAL;
            }
            case "EVENT_RECEIVED" -> {
                // Receiving from an endpoint should be SERVER for messaging/routing components
                // Exception: platform-http and servlet endpoints are INTERNAL because Quarkus Vert.x
                // already creates SERVER spans for HTTP requests
                if (endpointUri != null && (endpointUri.startsWith("platform-http:")
                        || endpointUri.startsWith("servlet:"))) {
                    yield SpanKind.INTERNAL;
                }
                if (endpointUri != null && isMessagingEndpoint(endpointUri)) {
                    yield SpanKind.SERVER;
                }
                yield SpanKind.INTERNAL;
            }
            default ->
                // Processors and other operations are INTERNAL
                SpanKind.INTERNAL;
            };
        }

        /**
         * Check if an endpoint is a messaging/routing endpoint that should use CLIENT/SERVER span kinds.
         * HTTP and other transport components create their own client spans, so we keep them as INTERNAL.
         */
        private boolean isMessagingEndpoint(String endpointUri) {
            return endpointUri.startsWith("direct:")
                    || endpointUri.startsWith("seda:")
                    || endpointUri.startsWith("vm:")
                    || endpointUri.startsWith("jms:")
                    || endpointUri.startsWith("kafka:")
                    || endpointUri.startsWith("amqp:")
                    || endpointUri.startsWith("rabbitmq:");
        }

        @Override
        public void activate(Span span) {
            OpenTelemetrySpanAdapter otelSpan = (OpenTelemetrySpanAdapter) span;
            otelSpan.makeCurrent();
        }

        @Override
        public void deactivate(Span span) {
            OpenTelemetrySpanAdapter otelSpan = (OpenTelemetrySpanAdapter) span;
            otelSpan.end();
        }

        @Override
        public void close(Span span) {
            OpenTelemetrySpanAdapter otelSpan = (OpenTelemetrySpanAdapter) span;
            otelSpan.close();
        }

        @Override
        public void inject(Span span, SpanContextPropagationInjector injector, boolean includeTracing) {
            OpenTelemetrySpanAdapter otelSpan = (OpenTelemetrySpanAdapter) span;
            Context ctx = Context.current().with(otelSpan.getSpan());
            if (otelSpan.getBaggage() != null) {
                ctx = ctx.with(otelSpan.getBaggage());
            }
            contextPropagators.getTextMapPropagator().inject(ctx, injector,
                    (carrier, key, value) -> carrier.put(key, value));
            if (includeTracing) {
                injector.put(org.apache.camel.telemetry.Tracer.TRACE_HEADER,
                        otelSpan.getSpan().getSpanContext().getTraceId());
                injector.put(org.apache.camel.telemetry.Tracer.SPAN_HEADER,
                        otelSpan.getSpan().getSpanContext().getSpanId());
            }
        }
    }
}
