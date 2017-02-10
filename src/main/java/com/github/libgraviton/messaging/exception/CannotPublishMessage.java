package com.github.libgraviton.messaging.exception;

import java.io.IOException;

public class CannotPublishMessage extends IOException {

    private String mqMessage;

    public CannotPublishMessage(String mqMessage, Exception cause) {
        super(String.format("Cannot publish message: '%s'", mqMessage), cause);
        this.mqMessage = mqMessage;
    }

    public String getMqMessage() {
        return mqMessage;
    }

}
