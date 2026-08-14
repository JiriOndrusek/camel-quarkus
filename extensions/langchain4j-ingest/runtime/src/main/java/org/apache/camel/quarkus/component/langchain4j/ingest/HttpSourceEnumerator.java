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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.quarkus.component.support.langchain4j.ingest.SyncPassRunner;

/**
 * The {@code http} source: one URL is one document. The document id is the URL. Tier-1
 * fingerprint comes from {@code ETag} / {@code Last-Modified} response headers, checked with a
 * cheap {@code HEAD} request so an unchanged document costs no body transfer; the body is only
 * fetched when change detection cannot skip. A {@code 404}/{@code 410} means the document
 * disappeared: the listing is empty and reconciliation removes it (the pass interlock applies).
 * Any other error fails the enumeration, which aborts the pass — an unreachable server must
 * never look like a disappeared document.
 */
class HttpSourceEnumerator {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String url;

    HttpSourceEnumerator(String url) {
        this.url = url;
    }

    Map<String, SyncPassRunner.SourceDocument> enumerate() throws IOException, InterruptedException {
        HttpResponse<Void> head = client.send(
                HttpRequest.newBuilder(URI.create(url)).method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(30)).build(),
                HttpResponse.BodyHandlers.discarding());

        Map<String, SyncPassRunner.SourceDocument> listing = new LinkedHashMap<>();
        if (head.statusCode() == 404 || head.statusCode() == 410) {
            return listing; // gone: reconciliation deletes, behind the interlock
        }
        if (head.statusCode() >= 400) {
            throw new IOException("HEAD " + url + " returned " + head.statusCode()
                    + " — enumeration failed, pass aborted (an unreachable document is not a deleted one)");
        }

        String fingerprint = fingerprintOf(head);
        listing.put(url, new SyncPassRunner.SourceDocument(fingerprint, this::fetchBody));
        return listing;
    }

    private static String fingerprintOf(HttpResponse<?> response) {
        String etag = response.headers().firstValue("ETag").orElse(null);
        if (etag != null) {
            return "etag:" + etag;
        }
        return response.headers().firstValue("Last-Modified")
                .map(lastModified -> "lm:" + lastModified)
                .orElse(null); // no cheap signal: tier-2 content hash takes over
    }

    private String fetchBody() {
        try {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(60)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GET " + url + " returned " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("GET " + url + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GET " + url + " interrupted", e);
        }
    }
}
