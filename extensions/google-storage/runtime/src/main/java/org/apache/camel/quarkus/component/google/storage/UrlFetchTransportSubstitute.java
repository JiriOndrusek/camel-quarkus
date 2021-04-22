package org.apache.camel.quarkus.component.google.storage;

import java.io.IOException;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = UrlFetchTransport.class)
public final class UrlFetchTransportSubstitute {

    @Substitute
    protected Object buildRequest(String method, String url) throws IOException {
        throw new IllegalStateException("3333333333333333333333333333333333");
    }

}
