package com.github.libgraviton.messaging.exception;

import com.github.libgraviton.messaging.MessageAcknowledger;

import java.io.IOException;

public class CannotAcknowledgeMessage extends IOException {

    private MessageAcknowledger acknowledger;

    private String messageId;

    public CannotAcknowledgeMessage(MessageAcknowledger acknowledger, String messageId, String reason) {
        this(acknowledger, messageId, reason, null);
    }

    public CannotAcknowledgeMessage(MessageAcknowledger acknowledger, String messageId, Exception cause) {
        this(acknowledger, messageId, "An com.github.libgraviton.messaging.exception occurred", cause);
    }

    private static String getErrorMessage(MessageAcknowledger acknowledger, String messageId, String reason) {
        if (messageId == null) {
            return String.format(
                "Acknowledger '%s' is unable to acknowledge message. Reason: '%s'.",
                acknowledger,
                reason);
        } else {
            return String.format(
                "Acknowledger '%s' is unable to acknowledge message with id '%s'. Reason: '%s'.",
                acknowledger,
                messageId,
                reason
            );
        }
    }

    private CannotAcknowledgeMessage(
            MessageAcknowledger acknowledger,
            String messageId,
            String reason,
            Exception cause
    ) {
        super(getErrorMessage(acknowledger,messageId,reason),cause);

        this.acknowledger = acknowledger;
        this.messageId = messageId;
    }

    public MessageAcknowledger getAcknowledger() {
        return acknowledger;
    }

    public String getMessageId() {
        return messageId;
    }

}
