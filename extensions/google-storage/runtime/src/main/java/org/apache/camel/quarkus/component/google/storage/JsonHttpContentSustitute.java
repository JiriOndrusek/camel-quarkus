package org.apache.camel.quarkus.component.google.storage;

import com.google.api.client.http.json.JsonHttpContent;

//@TargetClass(value = JsonHttpContent.class)
public final class JsonHttpContentSustitute {

    /** Wrapper key for the JSON content or {@code null} for none. */
    //    @Alias
    private String wrapperKey;

    //    @Substitute
    public JsonHttpContent setWrapperKey(String wrapperKey) {
        this.wrapperKey = wrapperKey;
        System.out.println(">>>>>>>>>>>. setting wraping key " + wrapperKey);
        return null;
    }
}
