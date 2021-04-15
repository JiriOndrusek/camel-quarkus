package org.apache.camel.quarkus.component.google.storage;

import com.google.cloud.storage.Bucket;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(value = Bucket.class)
public final class BucketSubstitution {

    @Substitute
    public com.google.api.services.storage.model.Bucket setId(java.lang.String id) {
        System.out.println("3333333333333 - id: " + id);
        return null;
    }
}
