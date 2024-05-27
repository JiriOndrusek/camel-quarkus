package org.apache.camel.quarkus.test.junit5.patterns;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.test.junit5.DebugBreakpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDebugBreakpoint extends DebugBreakpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TestDebugBreakpoint.class);

    @Override
    protected void debugBefore(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label) {
        // this method is invoked before we are about to enter the given
        // processor
        // from your Java editor you can add a breakpoint in the code line
        // below
        LOG.info("Before {} with body {}", definition, exchange.getIn().getBody());
    }

    @Override
    protected void debugAfter(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label,
            long timeTaken) {

    }
}
