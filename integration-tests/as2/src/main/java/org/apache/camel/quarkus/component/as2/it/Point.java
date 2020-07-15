package org.apache.camel.quarkus.component.as2.it;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.http.entity.ContentType;

public class Point {

    private AS2MessageStructure messageStructure;
    private String messageStructureKey;
    private ContentType contentType;
    private String contentTypeKey;
    private Map<String, Object> headers = new HashMap<>();

    public AS2MessageStructure getMessageStructure() {
        return messageStructure;
    }

    public void setMessageStructure(AS2MessageStructure messageStructure) {
        this.messageStructure = messageStructure;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void addField(String key, Object value) {
        this.headers.put(key, value);
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getMessageStructureKey() {
        return messageStructureKey;
    }

    public void setMessageStructureKey(String messageStructureKey) {
        this.messageStructureKey = messageStructureKey;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getContentTypeKey() {
        return contentTypeKey;
    }

    public void setContentTypeKey(String contentTypeKey) {
        this.contentTypeKey = contentTypeKey;
    }

    public void applyHeadersTypeSafe(Map<String, Object> headers) {
        this.headers = headers;
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getValue() instanceof AS2MessageStructure) {
                setMessageStructure((AS2MessageStructure) entry.getValue());
                setMessageStructureKey(entry.getKey());
            }
            //            } else if(entry.getValue() instanceof ContentType) {
            //                setContentType((ContentType) entry.getValue());
            //                setContentTypeKey(entry.getKey());
            //            } else if(!(entry.getValue() instanceof String)) {
            //                throw new IllegalArgumentException("Type is not supported");
            //            }
        }
    }

    public Map<String, Object> applyHeadersTypeSafe() {
        Map<String, Object> retVal = new HashMap<>(headers);
        if (getMessageStructure() != null) {
            retVal.put(getMessageStructureKey(), getMessageStructure());
        }
        retVal.put("CamelAS2.ediMessageContentType",
                org.apache.http.entity.ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII));
        return retVal;
    }

}
