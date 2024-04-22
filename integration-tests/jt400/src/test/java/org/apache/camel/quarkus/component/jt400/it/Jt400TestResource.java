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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSKey;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.KeyedDataQueueEntry;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;

public class Jt400TestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(Jt400TestResource.class);

    public static enum RESOURCE_TYPE {
        messageQueue,
        keyedDataQue,
        lifoQueueu,
        replyToQueueu;
    }

    private static final Optional<String> JT400_CLEAR_ALL = ConfigProvider.getConfig().getOptionalValue("cq.jt400.clear-all",
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

    //depth of repetitive reads for lifo queue clearing
    private final static int CLEAR_DEPTH = 100;
    public final static String LOCK_KEY = "cq.jt400.global-lock";
    //5 minute timeout to obtain a log for the tests execution
    private final static int LOCK_TIMEOUT = 30000;

    //todo rename to global connection
    private static AS400 lockAs400;

    @Override
    public Map<String, String> start() {
        //no need to start, as the instance already exists
        return Collections.emptyMap();
    }

    @Override
    public void stop() {

        //clear data after tests and before unlocking
        try {
            //            System.out.println("--------------------------------");
            //            System.out.println("------------- clear -------------");
            //            System.out.println("--------------------------------");
            //            CLIENT_HELPER.clear();
            CLIENT_HELPER.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        @Override
        public boolean clear() throws Exception {
            Thread.sleep(1000);
            //clear only once
            if (cleared) {
                return false;
            }
            //            boolean all = JT400_CLEAR_ALL.isPresent() && Boolean.parseBoolean(JT400_CLEAR_ALL.get());
            boolean all = true;
            if (all) {
                LOGGER.debug("Attention! Clearing all data.");
            }

            //            try (AS400 as400 = createAs400()) {
            AS400 as400 = lockAs400;
            //reply-to queue
            MessageQueue rq = new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));

            if (all) {
                rq.remove();
            } else if (toRemove.containsKey(RESOURCE_TYPE.replyToQueueu)) {
                clearMessageQueue(RESOURCE_TYPE.replyToQueueu, rq);
            }

            //message queue
            MessageQueue mq = new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE));
            if (all) {
                mq.remove();
            } else if (toRemove.containsKey(RESOURCE_TYPE.messageQueue)) {
                clearMessageQueue(RESOURCE_TYPE.messageQueue, mq);
            }

            //lifo queue
            DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
            if (all) {
                for (int i = 01; i < CLEAR_DEPTH; i++) {
                    if (dq.read() == null) {
                        break;
                    }
                }
            } else if (toRemove.containsKey(RESOURCE_TYPE.lifoQueueu)) {
                for (Object entry : toRemove.get(RESOURCE_TYPE.lifoQueueu)) {
                    List<byte[]> otherMessages = new LinkedList<>();
                    DataQueueEntry dqe = dq.read();
                    while (dqe != null && !(entry.equals(dqe.getString())
                            || entry.equals(new String(dqe.getData(), StandardCharsets.UTF_8)))) {
                        otherMessages.add(dqe.getData());
                        dqe = dq.read();
                    }
                    //write back other messages in reverse order (it is a lifo)
                    Collections.reverse(otherMessages);
                    for (byte[] msg : otherMessages) {
                        dq.write(msg);
                    }
                }
            }

            //keyed queue
            KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));
            if (all) {
                kdq.clear();
            } else if (toRemove.containsKey(RESOURCE_TYPE.keyedDataQue)) {
                for (Object entry : toRemove.get(RESOURCE_TYPE.keyedDataQue)) {
                    kdq.clear((String) entry);
                }
            }
            //            }

            return true;
        }

        private void clearMessageQueue(RESOURCE_TYPE type, MessageQueue mq) throws Exception,
                ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
            if (!toRemove.get(type).isEmpty()) {
                List<QueuedMessage> msgs = Collections.list(mq.getMessages());
                Map<String, Set<byte[]>> keys = new HashMap<>();
                for (QueuedMessage queuedMessage : msgs) {
                    if (!keys.containsKey(queuedMessage.getText())) {
                        keys.put(queuedMessage.getText(), new HashSet<>());
                    }
                    keys.get(queuedMessage.getText()).add(queuedMessage.getKey());
                }
                System.out.println(type + " before: " + msgs.size());
                for (Object entry : toRemove.get(type)) {
                    LOGGER.debug("Removing msg " + entry + " from the queue " + type);
                    if (entry instanceof String) {
                        for (byte[] key : keys.get((String) entry)) {
                            mq.remove(key);
                        }
                    } else {
                        mq.remove((byte[]) entry);
                    }
                }
                System.out.println(type + " after: " + Collections.list(mq.getMessages()).size());
            }
        }

        /**
         * Locking mechanism is based on Integrated file system and it file locking (which is part of JTOpen)
         *
         * File <i>/home/${JT400_USERNAME}/lock</i> has to exists.
         * Each lock ateempt asks for lock of inputStream for that file.
         * At the end of the execution the lock is released.
         *
         * Important!
         * The underlying AS400 (used for opening the stream) has to stay connected, otherwise the lock is released.
         */
        @Override
        public void lock() throws Exception {

            if (lockKey == null) {
                lockAs400 = createAs400();
                lockFile = new IFSFileInputStream(lockAs400, "/home/REDHAT5/lock");

                LOGGER.debug("Asked for lock.");

                Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(LOCK_TIMEOUT, TimeUnit.SECONDS)
                        .until(() -> {
                            try {
                                lockKey = lockFile.lock(8l);
                            } catch (Exception e) {
                                //lock was not acquired
                                return false;
                            }
                            LOGGER.debug("Acquired lock.");
                            return true;

                        },
                                Matchers.is(true));
            }
        }

        @Override
        public void unlock() throws Exception {
            if (lockKey != null) {
                lockFile.unlock(lockKey);
                LOGGER.debug("Released lock ");
                lockKey = null;
                lockAs400.close();
                lockAs400 = null;
                lockFile = null;
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

                sb.append("\n* KEYED DATA QUEUE\n");
                KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));
                KeyedDataQueueEntry kdqe = kdq.peek(LOCK_KEY);
                sb.append("\tlock: " + (kdqe == null ? "null" : kdqe.getString()));
                return sb.toString();

            } finally {
                as400.close();
            }
        }

        public void sendInquiry(String msg) throws Exception {
            AS400 as400 = createAs400();
            try {
                new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).sendInquiry(msg,
                        getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
            } finally {
                as400.close();
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

    //------------------- clear listeners ------------------------------

    boolean clear() throws Exception;

    //----------------------- locking

    void lock() throws Exception;

    void unlock() throws Exception;

    String dumpQueues() throws Exception;

}
