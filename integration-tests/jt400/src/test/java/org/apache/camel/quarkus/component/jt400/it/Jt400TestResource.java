package org.apache.camel.quarkus.component.jt400.it;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;

public class Jt400TestResource implements QuarkusTestResourceLifecycleManager {

    private static final Optional<String> JT400_CLEAR_ALL = ConfigProvider.getConfig().getOptionalValue("cq.jt400.clear-all", String.class);
    private static final String JT400_URL = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
    private static final String JT400_USERNAME = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
    private static final String JT400_PASSWORD = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
    private static final String JT400_LIBRARY = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
    private static final String JT400_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.message-queue",
            String.class);
    private static final String JT400_REPLY_TO_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.message-replyto-queue",
            String.class);
    private static final String JT400_LIFO_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.lifo-queue",
            String.class);
    private static final String JT400_KEYED_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.keyed-queue", String.class);

    private final int CLEAR_DEPTH = 100;

    private AS400 as400;

    private Set<RunnableWithException> clearTeasks = new HashSet<>();
    private Jt400ClientHelper clientHelper;

    @Override
    public Map<String, String> start() {
        //create client
        as400 = new AS400(JT400_URL, JT400_USERNAME, JT400_PASSWORD);
        //no need to start, as the instance already exists
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (as400 != null) {
            if(JT400_CLEAR_ALL.isPresent() && Boolean.parseBoolean(JT400_CLEAR_ALL.get())) {
                try {
                    clientHelper.clearAll();
                } catch (Exception e) {
                    //todo log
                    e.printStackTrace();
                }
            } else {
                clearTeasks.forEach(r -> {
                    try {
                        r.run();
                    } catch (Exception e) {
                        //todo log
                        e.printStackTrace();
                    }
                });
            }
            as400.close();
        }
    }

    private String getObjectPath(String object) {
        return String.format("/QSYS.LIB/%s.LIB/%s", JT400_LIBRARY, object);
    }

    @Override
    public void inject(Object testInstance) {
        if (testInstance instanceof Jt400Test) {
            ((Jt400Test) testInstance).setClientHelper(clientHelper = new Jt400ClientHelper() {

                private String key1ForKeyedDataQueue = null, key2ForKeyedDataQueue = null;

                @Override
                public QueuedMessage getReplyToQueueMessage(String msg) throws Exception {
                    return getQueueMessage(JT400_REPLY_TO_MESSAGE_QUEUE, msg);
                }

                @Override
                public QueuedMessage getQueueMessage(String msg) throws Exception {
                    return getQueueMessage(JT400_MESSAGE_QUEUE, msg);
                }

                private QueuedMessage getQueueMessage(String queue, String msg) throws Exception {
                    MessageQueue messageQueue = new MessageQueue(as400,
                            getObjectPath(queue));
                    Enumeration<QueuedMessage> msgs = messageQueue.getMessages();

                    while (msgs.hasMoreElements()) {
                        QueuedMessage queuedMessage = msgs.nextElement();

                        if (msg.equals(queuedMessage.getText())) {
                            return queuedMessage;
                        }
                    }
                    return null;
                }

                @Override
                public DataQueueEntry peekLifoQueueEntry() throws Exception {

                    DataQueue dataQueue = new DataQueue(as400,
                            getObjectPath(JT400_LIFO_QUEUE));
                    return dataQueue.peek();
                }

                public void clearLifoDataQueue(DataQueueEntry entry) {
                    clearTeasks.add(() -> {
                        DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
                        DataQueueEntry peek = dq.peek();
                        if(peek != null && Arrays.equals(peek.getData(), entry.getData())){
                            dq.read();
                        }
                    });
                }

                @Override
                public void addQueueMessageKeyToDelete(byte[] key) {
                    clearTeasks.add(() -> {
                        new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE))
                                .remove(key);
                    });
                }

                @Override
                public void addReplyToMessageKeyToDelete(byte[] key) {
                    clearTeasks.add(() -> {
                        new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE))
                                .remove(key);
                    });
                }

                @Override
                public String getKey1ForKeyedDataQueue() {
                    return key1ForKeyedDataQueue;
                }

                @Override
                public void addKeyedDataQueueKey1ToDelete(String key1ForKeyedDataQueue) {
                    this.key1ForKeyedDataQueue = key1ForKeyedDataQueue;
                    clearTeasks.add(() -> {
                        new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE))
                                .clear(key1ForKeyedDataQueue);
                    });
                }

                @Override
                public String getKey2ForKeyedDataQueue() {
                    return key2ForKeyedDataQueue;
                }

                @Override
                public void addKeyedDataQueueKey2ToDelete(String key2ForKeyedDataQueue) {
                    this.key2ForKeyedDataQueue = key2ForKeyedDataQueue;
                    clearTeasks.add(() -> {
                        new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE))
                                .clear(key2ForKeyedDataQueue);
                    });
                }


                @Override
                public void clearAll() throws Exception {
                    //message queue
                    new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE)).remove();

                    //lifo queue
                    DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
                    for(int i = 01; i< CLEAR_DEPTH; i++) {
                        if(dq.read() == null) {
                            break;
                        }
                    }
                    //reply-to queue
                    new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).remove();

                    //keyed queue
                    new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE)).clear();
                }
            });

        }
    }


}

interface Jt400ClientHelper {

    QueuedMessage getQueueMessage(String msg) throws Exception;

    QueuedMessage getReplyToQueueMessage(String msg) throws Exception;

    DataQueueEntry peekLifoQueueEntry() throws Exception;

    String getKey2ForKeyedDataQueue();

    String getKey1ForKeyedDataQueue();

    //------------------- clear listeners ------------------------------

    void addQueueMessageKeyToDelete(byte[] key);

    void addReplyToMessageKeyToDelete(byte[] key);

    void addKeyedDataQueueKey1ToDelete(String key1);

    void addKeyedDataQueueKey2ToDelete(String key1);

    void clearLifoDataQueue(DataQueueEntry entry);

    void clearAll() throws Exception;

}

@FunctionalInterface
interface RunnableWithException {
    public abstract void run() throws Exception;
}
