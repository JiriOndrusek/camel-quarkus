package org.apache.camel.quarkus.support.google.cloud.graal;

import java.util.function.BooleanSupplier;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import static org.apache.camel.quarkus.support.google.cloud.graal.DefaultHttpTransportFactorySubstitutions.DefaultHttpTransportFactoryPresent;

@TargetClass(className = "com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory", onlyWith = DefaultHttpTransportFactoryPresent.class)
public final class DefaultHttpTransportFactorySubstitutions {

    @Substitute
    public HttpTransport create() {
        // Suppress creation of UrlFetchTransport for GAE which is not supported in native mode
        return new NetHttpTransport();
    }

    static final class DefaultHttpTransportFactoryPresent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader()
                        .loadClass("com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
