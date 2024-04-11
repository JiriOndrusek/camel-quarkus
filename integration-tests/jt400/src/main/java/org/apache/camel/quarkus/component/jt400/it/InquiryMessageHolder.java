package org.apache.camel.quarkus.component.jt400.it;

import jakarta.inject.Singleton;

@Singleton
public class InquiryMessageHolder {

    private String messageText;

    private boolean processed = false;

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
