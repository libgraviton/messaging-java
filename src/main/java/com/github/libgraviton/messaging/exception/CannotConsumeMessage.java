package com.github.libgraviton.messaging.exception;

import java.io.IOException;

public class CannotConsumeMessage extends IOException {

    private String messageId;

    private String mqMessage;

    public CannotConsumeMessage(String messageId, String mqMessage, String reason) {
        super(String.format(
                "Unable to consume mqMessage with id '%s': '%s'. Reason: '%s'",
                messageId,
                mqMessage,
                reason
        ));
        this.messageId = messageId;
        this.mqMessage = mqMessage;
    }

    public CannotConsumeMessage(String messageId, String mqMessage, Exception cause) {
        super(
                String.format(
                        "Unable to consume mqMessage with id '%s': '%s'. An Exception occurred.",
                        messageId,
                        mqMessage
                ),
                cause
        );
        this.messageId = messageId;
        this.mqMessage = mqMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMqMessage() {
        return mqMessage;
    }
}
