package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.exception.CannotConsumeMessage;

public interface Consumer {

    void consume(String messageId, String message) throws CannotConsumeMessage;

}
