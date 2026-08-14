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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.support.langchain4j.ingest.SyncPassRunner;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * The {@code s3} source: object key is the document id, the ETag is the change fingerprint —
 * available from the listing, before any object is downloaded. Bodies are fetched lazily, only
 * for documents change detection cannot skip. Goes through the Camel aws2-s3 component (an
 * optional dependency validated at build time), so credentials, regions and S3-compatible
 * endpoints behave exactly as Camel users know them — without any Camel visible to this
 * extension's users. This class is only loaded when a pipeline declares {@code source.type=s3}.
 */
class S3SourceEnumerator {

    private final ProducerTemplate producerTemplate;
    private final String baseUri;
    private final String prefix;

    S3SourceEnumerator(ProducerTemplate producerTemplate, IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig source,
            String pipeline) {
        this.producerTemplate = producerTemplate;
        String bucket = source.bucket().orElseThrow(() -> new IllegalStateException(
                "Ingestion pipeline '" + pipeline + "' has source type 's3' but no bucket. "
                        + "Set quarkus.camel.ai.ingest." + pipeline + ".source.bucket"));
        StringBuilder uri = new StringBuilder("aws2-s3://").append(bucket).append("?");
        source.region().ifPresent(region -> uri.append("region=").append(region).append('&'));
        source.accessKey().ifPresent(key -> uri.append("accessKey=RAW(").append(key).append(")&"));
        source.secretKey().ifPresent(key -> uri.append("secretKey=RAW(").append(key).append(")&"));
        source.endpointOverride().ifPresent(endpoint -> uri.append("overrideEndpoint=true&uriEndpointOverride=")
                .append(endpoint).append("&forcePathStyle=true&"));
        this.baseUri = uri.toString();
        this.prefix = source.prefix().orElse(null);
    }

    Map<String, SyncPassRunner.SourceDocument> enumerate() {
        @SuppressWarnings("unchecked")
        List<S3Object> objects = producerTemplate.requestBody(baseUri + "operation=listObjects", null, List.class);

        Map<String, SyncPassRunner.SourceDocument> listing = new LinkedHashMap<>();
        for (S3Object object : objects) {
            String key = object.key();
            if (prefix != null && !key.startsWith(prefix)) {
                continue;
            }
            String fingerprint = "etag:" + object.eTag();
            listing.put(key, new SyncPassRunner.SourceDocument(fingerprint,
                    () -> producerTemplate.requestBodyAndHeader(baseUri + "operation=getObject", null,
                            "CamelAwsS3Key", key, String.class)));
        }
        return listing;
    }
}
