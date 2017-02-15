package com.github.libgraviton.messaging.exception;

import java.io.IOException;

public class CannotPublishMessage extends IOException {

    private String mqMessage;

    public CannotPublishMessage(String mqMessage, String reason) {
        super(String.format("Cannot publish message '%s'. Reason: '%s'", mqMessage, reason));
        this.mqMessage = mqMessage;
    }

    public CannotPublishMessage(String mqMessage, Throwable cause) {
        this(mqMessage, "An exception occurred.");
        initCause(cause);
    }

    public String getMqMessage() {
        return mqMessage;
    }

}
