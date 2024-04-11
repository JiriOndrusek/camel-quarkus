package org.apache.camel.quarkus.component.jt400.it;

import jakarta.inject.Singleton;

@Singleton
public class InquiryMessageHolder {

    private String messageText;

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
