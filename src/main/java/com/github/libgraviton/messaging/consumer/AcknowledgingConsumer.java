package com.github.libgraviton.messaging.consumer;

import com.github.libgraviton.messaging.MessageAcknowledger;

public interface AcknowledgingConsumer extends Consumer {

    void setAcknowledger(MessageAcknowledger acknowledger);

}
