package com.github.libgraviton.messaging;

public interface AcknowledgingConsumer extends Consumer {

    void setAcknowledger(MessageAcknowledger acknowledger);

}
