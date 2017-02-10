package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.exception.CannotAcknowledgeMessage;

public interface MessageAcknowledger {

    void acknowledge(String messageId) throws CannotAcknowledgeMessage;

}
