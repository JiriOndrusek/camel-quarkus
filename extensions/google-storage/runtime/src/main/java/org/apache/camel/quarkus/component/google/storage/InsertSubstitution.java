package org.apache.camel.quarkus.component.google.storage;

//@TargetClass(value = Storage.Objects.Insert.class)
//@Substitute
public final class InsertSubstitution {

    protected InsertSubstitution(java.lang.String bucket, com.google.api.services.storage.model.StorageObject content) {
        System.out.println("--------------- insert constructor -------------------");
    }
}
