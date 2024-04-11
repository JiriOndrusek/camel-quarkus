package org.apache.camel.quarkus.component.jt400.it;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Jt400TestResource implements QuarkusTestResourceLifecycleManager {

    private static final String JT400_URL = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
    private static final String JT400_USERNAME = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
    private static final String JT400_PASSWORD = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
    private static final String JT400_LIBRARY = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
    private static final String JT400_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.message-queue", String.class);

    private AS400 as400;

    private Set<RunnableWithException> clearTeasks = new HashSet<>();

    @Override
    public Map<String, String> start() {
        //create client
        as400 = new AS400(JT400_URL, JT400_USERNAME, JT400_PASSWORD);
        //no need to start, as the instance already exists
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if(as400 != null) {
            clearTeasks.forEach(r -> {
                try {
                    r.run();
                } catch (Exception e) {
                    //todo log
                }
            });
            as400.close();
        }
    }

    @Override
    public void inject(Object testInstance) {
        if(testInstance instanceof  Jt400Test) {
            ((Jt400Test) testInstance).setClientHelper(new Jt400ClientHelper() {

                @Override
                public QueuedMessage getQueueMessage(String msg) throws Exception {
                    MessageQueue messageQueue = new MessageQueue(as400, String.format("/QSYS.LIB/%s.LIB/%s", JT400_LIBRARY, JT400_MESSAGE_QUEUE));
                    Enumeration<QueuedMessage> msgs = messageQueue.getMessages();

                    while(msgs.hasMoreElements()) {
                        QueuedMessage queuedMessage = msgs.nextElement();;
                        if(msg.equals(queuedMessage.getText())) {
                            return queuedMessage;
                        }
                    }
                    return null;
                }

                @Override
                public void addQueuMessageKeyToDelete(byte[] key) {
                    clearTeasks.add(() -> {
                        new MessageQueue(as400, String.format("/QSYS.LIB/%s.LIB/%s", JT400_LIBRARY, JT400_MESSAGE_QUEUE)).remove(key);
                    });
                }
            });
        }
    }
}

interface Jt400ClientHelper {

    QueuedMessage getQueueMessage(String msg) throws Exception;

    void addQueuMessageKeyToDelete(byte[] key);
}

@FunctionalInterface
interface RunnableWithException {
    public abstract void run() throws Exception;
}
