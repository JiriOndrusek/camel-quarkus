package org.apache.camel.quarkus.component.google.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.google.cloud.storage.spi.v1.HttpStorageRpc;
import com.google.cloud.storage.spi.v1.StorageRpc;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracer;

@TargetClass(value = HttpStorageRpc.class)
public final class HttpStorageRpcSubstitute {

    @Alias
    private Storage storage;
    @Alias
    private Tracer tracer;

    @Alias
    private Span startSpan(String spanName) {
        return null;
    }

    @Substitute
    public StorageObject create(
            StorageObject storageObject, final InputStream content, Map<StorageRpc.Option, ?> options) {
        System.out.println("so: " + storageObject);
        System.out.println("co: " + content);
        System.out.println("op: " + options);

        Span span = startSpan(String.format(
                "%s.%s.%s", "Sent", HttpStorageRpc.class.getName(), "create(StorageObject,InputStream,Map)"));
        System.out.println(">>> 1: span started - " + span);
        Scope scope = tracer.withSpan(span);
        System.out.println(">>> 2: scope - " + scope);
        try {
            Storage.Objects.Insert insert = storage
                    .objects()
                    .insert(
                            storageObject.getBucket(),
                            storageObject,
                            new InputStreamContent("application/octet-stream", content));
            System.out.println(">>> 3: insert - " + insert);
            System.out.println(">>> 3: insert b - " + insert.getBucket());
            insert.getMediaHttpUploader().setDirectUploadEnabled(true);
            Storage.Objects.Insert i2 = insert
                    .setProjection("full")
                    .setPredefinedAcl(null)
                    .setIfMetagenerationMatch(null)
                    .setIfMetagenerationNotMatch(null)
                    .setIfGenerationMatch(null)
                    .setIfGenerationNotMatch(null)
                    .setUserProject(null)
                    .setKmsKeyName(null);
            System.out.println(">>> 6: before execute: " + i2);
            StorageObject so = i2.execute();
            System.out.println(">>> 4: executed - " + so);
            return so;
        } catch (IOException ex) {
            span.setStatus(Status.UNKNOWN.withDescription(ex.getMessage()));
            ex.printStackTrace();
        } finally {
            scope.close();
            //            span.end(HttpStorageRpcSpans.END_SPAN_OPTIONS);
        }

        return null;
    }
}
