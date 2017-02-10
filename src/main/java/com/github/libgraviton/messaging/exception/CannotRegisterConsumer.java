package com.github.libgraviton.messaging.exception;

import com.github.libgraviton.messaging.consumer.Consumer;

import java.io.IOException;

public class CannotRegisterConsumer extends IOException {

    private Consumer consumer;

    public CannotRegisterConsumer(Consumer consumer, String reason) {
        this(consumer, reason, null);
    }

    public CannotRegisterConsumer(Consumer consumer, Exception cause) {
        this(consumer, "An Exception occurred.", cause);
    }

    private CannotRegisterConsumer(Consumer consumer, String reason, Throwable cause) {
        super(String.format("Cannot register consumer '%s'. Reason: '%s'", consumer, reason), cause);
        this.consumer = consumer;
    }

    public Consumer getConsumer() {
        return consumer;
    }

}
