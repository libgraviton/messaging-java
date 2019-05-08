package com.github.libgraviton.messaging.consumer;

import com.rabbitmq.client.AMQP.BasicProperties;

public interface PropertyConsumer {

    void setBasicProperties(BasicProperties basicProperties);

}
