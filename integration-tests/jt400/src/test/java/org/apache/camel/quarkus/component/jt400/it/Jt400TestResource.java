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
package org.apache.camel.quarkus.component.jt400.it;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSKey;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.component.jt400.Jt400Component;
import org.apache.camel.component.jt400.Jt400Endpoint;
import org.apache.camel.component.jt400.Jt400MsgQueueService;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;

public class Jt400TestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(Jt400TestResource.class);

    public enum RESOURCE_TYPE {
        messageQueue,
        keyedDataQue,
        lifoQueueu,
        replyToQueueu;
    }

    static final Optional<String> JT400_CLEAR_ALL = ConfigProvider.getConfig().getOptionalValue("cq.jt400.clear-all",
            String.class);
    private static final String JT400_URL = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
    private static final String JT400_USERNAME = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
    private static final String JT400_PASSWORD = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
    private static final String JT400_LIBRARY = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
    private static final String JT400_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.message-queue",
            String.class);
    private static final String JT400_REPLY_TO_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue(
            "cq.jt400.message-replyto-queue",
            String.class);
    private static final String JT400_LIFO_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.lifo-queue",
            String.class);
    private static final String JT400_KEYED_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.keyed-queue", String.class);
    private static final Optional<String> JT400_LOCK_FILE = ConfigProvider.getConfig().getOptionalValue("cq.jt400.lock-file",
            String.class);

    //depth of repetitive reads for lifo queue clearing
    private final static int CLEAR_DEPTH = 100;
    //10 minute timeout to obtain a log for the tests execution
    private final static int LOCK_TIMEOUT = 600000;

    private static AS400 lockAs400;

    @Override
    public Map<String, String> start() {
        //no need to start, as the instance already exists
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        //no need to unlock, once the as400 connection is release, the lock is released
        if (lockAs400 != null) {
            lockAs400.close();
        }
    }

    private static String getObjectPath(String object) {
        return String.format("/QSYS.LIB/%s.LIB/%s", JT400_LIBRARY, object);
    }

    public static Jt400ClientHelper CLIENT_HELPER = new Jt400ClientHelper() {

        private boolean cleared = false;
        Map<RESOURCE_TYPE, Set<Object>> toRemove = new HashMap<>();

        IFSFileInputStream lockFile;
        IFSKey lockKey;

        @Override
        public QueuedMessage peekReplyToQueueMessage(String msg) throws Exception {
            return getQueueMessage(JT400_REPLY_TO_MESSAGE_QUEUE, msg);
        }

        private QueuedMessage getQueueMessage(String queue, String msg) throws Exception {
            AS400 as400 = createAs400();
            try {
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

            } finally {
                as400.close();
            }
        }

        @Override
        public void registerForRemoval(RESOURCE_TYPE type, Object value) {
            if (toRemove.containsKey(type)) {
                toRemove.get(type).add(value);
            } else {
                Set<Object> set = new HashSet<>();
                set.add(value);
                toRemove.put(type, set);
            }
        }

        public void testCPF2451() throws Exception {

            String routeConsumerUrl = String.format("jt400://%s:%s@%s%s", JT400_USERNAME, JT400_PASSWORD, JT400_URL,
                    "/QSYS.LIB/" + JT400_LIBRARY + ".LIB/");

            AS400ConnectionPool cp = new AS400ConnectionPool();
            Jt400Component comp = new Jt400Component();
            Jt400Endpoint consumerEndpoint = new Jt400Endpoint(routeConsumerUrl, comp, cp);
            consumerEndpoint.getConfiguration().setObjectPath(getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
            Jt400MsgQueueService consumerService = new Jt400MsgQueueService(consumerEndpoint);
            Thread.sleep(1000);

            consumerService.start();
            Thread.sleep(1000);

            String msg = "AAAAAAAAAAA" + RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);
            sendInquiry(msg);
            System.out.println("xxx: message '" + msg + "' written via client");
            Thread.sleep(1000);

            registerForRemoval(RESOURCE_TYPE.replyToQueueu, msg);

            MessageQueue consumerQueue = consumerService.getMsgQueue();
            Thread.sleep(1000);

            QueuedMessage consumedMessage = consumerQueue.receive(null, //message key
                    30, //timeout
                    "*OLD", // message action
                    MessageQueue.ANY);
            Thread.sleep(1000);
            System.out.println("xxx: message '" + consumedMessage + "' read");

            Thread.sleep(1000);

            Jt400Endpoint producerEndpoint = new Jt400Endpoint(routeConsumerUrl, comp, cp);
            producerEndpoint.getConfiguration().setObjectPath(getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
            Jt400MsgQueueService producerService = new Jt400MsgQueueService(consumerEndpoint);
            producerService.start();
            Thread.sleep(1000);
            MessageQueue producerQueue = producerService.getMsgQueue();
            String reply = "Reply to: " + consumedMessage.getText();
            //            registerForRemoval(RESOURCE_TYPE.replyToQueueu, reply);
            //            consumerService.stop();
            producerQueue.reply(consumedMessage.getKey(), reply);
            Thread.sleep(1000);

            //            producerService.stop();

            //check written message with client
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).until(
                    () -> peekReplyToQueueMessage(reply),
                    Matchers.notNullValue());
            LOGGER.debug("testInquiryMessageQueue: reply message confirmed by peek: " + reply);

            //            String reply;
            //            //reply to message
            ////            try (AS400 as400 = createAs400()) {
            //            {
            //                AS400 as400 = createAs400();
            //                MessageQueue queue = new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
            //
            //                queue.reply(qm.getKey(), reply );
            //                System.out.println("xxx: replied with:  " + reply);
            //            }
            //            Thread.sleep(1000);
            //

            clear();

            //            System.out.println("xxxxxxxxxxxxxx peek before: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));
            //
            //            AS400 as400 = createAs400();
            //            MessageQueue mq = new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE));
            //            mq.sendInformational(msg);
            //            //do not close connection
            //            System.out.println("xxxxxxxxxxxxxx peek after write: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));
            //            //try to delete with other client
            //            try (AS400 client2 = createAs400()) {
            //                MessageQueue mq2 = new MessageQueue(client2, getObjectPath(JT400_MESSAGE_QUEUE));
            //                mq2.remove();
            //            }
            //
            //            System.out.println("xxxxxxxxxxxxxx peek after clear: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));
        }

        public void testCPF2451_simple() throws Exception {

            String msg = "testing message";
            System.out.println("xxxxxxxxxxxxxx peek before: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));

            AS400 as400 = createAs400();
            MessageQueue mq = new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE));
            mq.sendInformational(msg);
            //do not close connection
            System.out.println("xxxxxxxxxxxxxx peek after write: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));
            //try to delete with other client
            try (AS400 client2 = createAs400()) {
                MessageQueue mq2 = new MessageQueue(client2, getObjectPath(JT400_MESSAGE_QUEUE));
                mq2.remove();
            }

            System.out.println("xxxxxxxxxxxxxx peek after clear: " + getQueueMessage(JT400_MESSAGE_QUEUE, msg));
        }

        @Override
        public boolean clear() {

            //clear only once
            if (cleared) {
                return false;
            }

            boolean all = JT400_CLEAR_ALL.isPresent();

            try (AS400 as400 = createAs400()) {

                MessageQueue mq = new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE));
                MessageQueue rq = new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
                DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
                KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));

                if (all) {
                    logError(() -> mq.remove());
                    logError(() -> rq.remove());
                    logError(() -> kdq.clear());

                    for (int i = 1; i < CLEAR_DEPTH; i++) {
                        if (logError(() -> dq.read()) == null) {
                            break;
                        }
                    }
                } else {
                    logError(() -> clearMessageQueue(RESOURCE_TYPE.messageQueue, mq));
                    logError(() -> clearMessageQueue(RESOURCE_TYPE.replyToQueueu, rq));
                    toRemove.getOrDefault(RESOURCE_TYPE.keyedDataQue, Collections.emptySet()).stream()
                            .forEach(entry -> logError(() -> kdq.clear((String) entry)));

                    if (toRemove.containsKey(RESOURCE_TYPE.lifoQueueu)) {
                        Set<Object> entriesToRemove = toRemove.get(RESOURCE_TYPE.lifoQueueu);
                        List<byte[]> otherMessages = new LinkedList<>();
                        for (int i = 1; i < CLEAR_DEPTH; i++) {
                            DataQueueEntry dqe = logError(() -> dq.read());
                            if (dqe == null) {
                                break;
                            }
                            try {
                                entriesToRemove.remove(dqe.getString());
                            } catch (UnsupportedEncodingException e) {
                                LOGGER.debug("Failed to decode entry from lifo queue.", e);
                            }
                            if (entriesToRemove.isEmpty()) {
                                break;
                            }
                        }

                        //write back other messages in reverse order (it is a lifo)
                        Collections.reverse(otherMessages);
                        for (byte[] msg : otherMessages) {
                            logError(() -> dq.write(msg));
                        }
                    }
                }
            }

            return true;
        }

        private void logError(org.jboss.resteasy.spi.RunnableWithException task) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.debug("Failed to clear queue.", e);
            }
        }

        private <T> T logError(SupplierWithException<T> task) {
            try {
                task.get();
            } catch (Exception e) {
                LOGGER.debug("Failed to clear queue.", e);
            }
            return null;
        }

        private void clearMessageQueue(RESOURCE_TYPE type, MessageQueue mq) throws AS400SecurityException,
                ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
            if (toRemove.containsKey(type) && !toRemove.get(type).isEmpty()) {
                List<QueuedMessage> msgs = Collections.list(mq.getMessages());
                Map<String, Set<byte[]>> textToBytes = msgs.stream()
                        .collect(Collectors.toMap(q -> q.getText(), q -> Collections.singleton(q.getKey()),
                                (v1, v2) -> {
                                    //merge sets in case of duplicated keys - which may happen
                                    Set<byte[]> retVal = new HashSet<>();
                                    retVal.addAll(v1);
                                    retVal.addAll(v2);
                                    return v2;
                                }));
                for (Object entry : toRemove.get(type)) {
                    if (entry instanceof String && textToBytes.containsKey((String) entry)) {
                        textToBytes.get((String) entry).stream().forEach(v -> {
                            try {
                                mq.remove(v);
                            } catch (Exception e) {
                                LOGGER.debug("Failed to remove key `" + entry + "` from replyTo queue", e);
                            }
                        });
                    } else if (entry instanceof byte[]) {
                        mq.remove((byte[]) entry);
                    }
                }
            }
        }

        /**
         * Locking is implemented via file locking, which is present in JTOpen.
         */
        @Override
        public void lock() throws Exception {

            //if no lock file is proposed, throw an error
            if (JT400_LOCK_FILE.isEmpty()) {
                throw new IllegalStateException("No file for locking is provided.");
            }

            if (lockKey == null) {
                lockAs400 = createAs400();
                lockFile = new IFSFileInputStream(lockAs400, JT400_LOCK_FILE.get());

                LOGGER.debug("Asked for lock.");

                Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(LOCK_TIMEOUT, TimeUnit.SECONDS)
                        .until(() -> {
                            try {
                                lockKey = lockFile.lock(1l);
                            } catch (Exception e) {
                                //lock was not acquired
                                return false;
                            }
                            LOGGER.debug("Acquired lock (for file `" + JT400_LOCK_FILE.get() + "`.");
                            return true;

                        },
                                Matchers.is(true));
            }
        }

        @Override
        public String dumpQueues() throws Exception {
            AS400 as400 = createAs400();
            try {
                StringBuilder sb = new StringBuilder();

                sb.append("\n* MESSAGE QUEUE\n");
                sb.append("\t" + Collections.list(new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE)).getMessages())
                        .stream().map(mq -> mq.getText()).sorted().collect(Collectors.joining(", ")));

                sb.append("\n* INQUIRY QUEUE\n");
                sb.append("\t" + Collections
                        .list(new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).getMessages())
                        .stream().map(mq -> mq.getText()).sorted().collect(Collectors.joining(", ")));

                sb.append("\n* LIFO QUEUE\n");
                DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
                DataQueueEntry dqe;
                List<byte[]> lifoMessages = new LinkedList<>();
                List<String> lifoTexts = new LinkedList<>();
                do {
                    dqe = dq.read();
                    if (dqe != null) {
                        lifoTexts.add(dqe.getString() + " (" + new String(dqe.getData(), StandardCharsets.UTF_8) + ")");
                        lifoMessages.add(dqe.getData());
                    }
                } while (dqe != null);

                //write back other messages in reverse order (it is a lifo)
                Collections.reverse(lifoMessages);
                for (byte[] msg : lifoMessages) {
                    dq.write(msg);
                }
                sb.append(lifoTexts.stream().collect(Collectors.joining(", ")));

                //there is no api to list keyed queue, without knowledge of keys
                return sb.toString();

            } finally {
                as400.close();
            }
        }

        public void sendInquiry(String msg) throws Exception {

            try (AS400 as400 = createAs400()) {
                new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).sendInquiry(msg,
                        getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
                as400.disconnectAllServices();
            }
        }
    };

    private static AS400 createAs400() {
        return new AS400(JT400_URL, JT400_USERNAME, JT400_PASSWORD);
    }

}

interface Jt400ClientHelper {

    void registerForRemoval(Jt400TestResource.RESOURCE_TYPE type, Object value);

    QueuedMessage peekReplyToQueueMessage(String msg) throws Exception;

    void sendInquiry(String msg) throws Exception;

    void testCPF2451() throws Exception;

    //------------------- clear listeners ------------------------------

    boolean clear();

    //----------------------- locking

    void lock() throws Exception;

    String dumpQueues() throws Exception;

}

@FunctionalInterface
interface SupplierWithException<T> {
    T get() throws Exception;
}
