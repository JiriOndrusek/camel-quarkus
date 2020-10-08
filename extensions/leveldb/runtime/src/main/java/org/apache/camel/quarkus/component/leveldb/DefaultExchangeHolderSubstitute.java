package org.apache.camel.quarkus.component.leveldb;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

//@RegisterForReflection
@TargetClass(value = DefaultExchangeHolderSubstitute.class)
final class DefaultExchangeHolderSubstitute {
    //
    //    @Alias
    //    private String exchangeId;
    //    @Alias
    //    private Object inBody;
    //    @Alias
    //    private Object outBody;
    //    @Alias
    //    private Map<String, Object> inHeaders;
    //    @Alias
    //    private Map<String, Object> outHeaders;
    //    @Alias
    //    private Map<String, Object> properties;
    //    @Alias
    //    private Exception exception;

    /**
     * No-args constructor is required for marsalling and unmarshalling
     */
    @Substitute
    public DefaultExchangeHolderSubstitute() {
    }

    //    @Substitute
    //    public String getExchangeId() {
    //        return exchangeId;
    //    }
    //
    //    @Substitute
    //    public void setExchangeId(String exchangeId) {
    //        this.exchangeId = exchangeId;
    //    }
    //
    //    @Substitute
    //    public Object getInBody() {
    //        return inBody;
    //    }
    //
    //    @Substitute
    //    public void setInBody(Object inBody) {
    //        this.inBody = inBody;
    //    }
    //
    //    @Substitute
    //    public Object getOutBody() {
    //        return outBody;
    //    }
    //
    //    @Substitute
    //    public void setOutBody(Object outBody) {
    //        this.outBody = outBody;
    //    }
    //
    //    @Substitute
    //    public Map<String, Object> getInHeaders() {
    //        return inHeaders;
    //    }
    //
    //    @Substitute
    //    public void setInHeaders(Map<String, Object> inHeaders) {
    //        this.inHeaders = inHeaders;
    //    }
    //
    //    @Substitute
    //    public Map<String, Object> getOutHeaders() {
    //        return outHeaders;
    //    }
    //
    //    @Substitute
    //    public void setOutHeaders(Map<String, Object> outHeaders) {
    //        this.outHeaders = outHeaders;
    //    }
    //
    //    @Substitute
    //    public Map<String, Object> getProperties() {
    //        return properties;
    //    }
    //
    //    @Substitute
    //    public void setProperties(Map<String, Object> properties) {
    //        this.properties = properties;
    //    }
    //
    //    @Substitute
    //    public Exception getException() {
    //        return exception;
    //    }
    //
    //    @Substitute
    //    public void setException(Exception exception) {
    //        this.exception = exception;
    //    }
}
