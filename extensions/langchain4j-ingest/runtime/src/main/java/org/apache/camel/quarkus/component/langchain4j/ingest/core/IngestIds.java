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
package org.apache.camel.quarkus.component.langchain4j.ingest.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Deterministic, UUID-formatted segment ids: the same (pipeline, documentId, segmentIndex) always
 * yields the same id, across runs and processes. On stores whose write is an upsert this makes
 * re-ingestion overwrite instead of duplicate; it is the primitive the sync mode of a later
 * release builds on. RFC 4122 name-based UUID, version 5 (SHA-1).
 */
public final class IngestIds {

    private static final UUID NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"); // RFC 4122 DNS ns

    private IngestIds() {
    }

    public static String segmentId(String pipeline, String documentId, int segmentIndex) {
        return uuidV5(pipeline + "|" + documentId + "|" + segmentIndex).toString();
    }

    static UUID uuidV5(String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(toBytes(NAMESPACE));
            sha1.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] hash = sha1.digest();
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x50); // version 5
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // IETF variant
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        byte[] out = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            out[i] = (byte) (msb >>> (8 * (7 - i)));
            out[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return out;
    }
}
