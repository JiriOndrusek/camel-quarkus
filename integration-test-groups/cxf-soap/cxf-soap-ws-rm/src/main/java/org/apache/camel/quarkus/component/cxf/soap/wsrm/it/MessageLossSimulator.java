package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ListIterator;

public class MessageLossSimulator extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(MessageLossSimulator.class.getName());
    private int appMessageCount;

    public MessageLossSimulator() {
        super("prepare-send");
        this.addBefore(MessageSenderInterceptor.class.getName());
    }

    private static String getAction(Object map) {
        if (map == null) {
            return null;
        } else {
            try {
                Object o = map.getClass().getMethod("getAction").invoke(map);
                return (String)o.getClass().getMethod("getValue").invoke(o);
            } catch (Throwable var2) {
                throw new Fault(var2);
            }
        }
    }

    public void handleMessage(Message message) throws Fault {
        Object maps = RMContextUtils.retrieveMAPs(message, false, true);
        String action = getAction(maps);
        if (!RMContextUtils.isRMProtocolMessage(action)) {
            ++this.appMessageCount;
            if (0 == this.appMessageCount % 2) {
                InterceptorChain chain = message.getInterceptorChain();
                ListIterator it = chain.getIterator();

                while(it.hasNext()) {
                    PhaseInterceptor<?> pi = (PhaseInterceptor)it.next();
                    if (MessageSenderInterceptor.class.getName().equals(pi.getId())) {
                        chain.remove(pi);
                        LOG.debug("Removed MessageSenderInterceptor from interceptor chain.");
                        break;
                    }
                }

                message.setContent(OutputStream.class, new MessageLossSimulator.WrappedOutputStream(message));
                message.getInterceptorChain().add(new AbstractPhaseInterceptor<Message>("prepare-send-ending") {
                    public void handleMessage(Message message) throws Fault {
                        try {
                            ((OutputStream)message.getContent(OutputStream.class)).close();
                        } catch (IOException var3) {
                            throw new Fault(var3);
                        }
                    }
                });
            }
        }
    }

    private class DummyOutputStream extends OutputStream {
        private DummyOutputStream() {
        }

        public void write(int b) throws IOException {
        }
    }

    private class WrappedOutputStream extends AbstractWrappedOutputStream {
        private Message outMessage;

        WrappedOutputStream(Message m) {
            this.outMessage = m;
        }

        protected void onFirstWrite() throws IOException {
            if (MessageLossSimulator.LOG.isDebugEnabled()) {
                Long nr = RMContextUtils.retrieveRMProperties(this.outMessage, true).getSequence().getMessageNumber();
                MessageLossSimulator.LOG.debug("Losing message {}", nr);
            }

            this.wrappedStream = MessageLossSimulator.this.new DummyOutputStream();
        }
    }
}
