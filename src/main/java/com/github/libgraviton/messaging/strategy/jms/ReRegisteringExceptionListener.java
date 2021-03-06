package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

class ReRegisteringExceptionListener extends RecoveringExceptionListener {

    private static final Logger LOG = LoggerFactory.getLogger(ReRegisteringExceptionListener.class);

    private Consumer consumer;

    ReRegisteringExceptionListener(JmsConnection connection, Consumer consumer) {
        super(connection);
        this.consumer = consumer;
    }

    @Override
    public void onException(JMSException e) {
        super.onException(e);
        try {
            LOG.info(String.format(
                    "Re-registering consumer '%s' on queue '%s'...",
                    consumer,
                    connection.getConnectionName())
            );
            connection.consume(consumer);
            LOG.info(String.format(
                    "Successfully re-registered consumer '%s' on queue '%s'.",
                    consumer,
                    connection.getConnectionName())
            );
        } catch (CannotRegisterConsumer registerException) {
            LOG.error(String.format(
                    "Re-registration of consumer '%s' on queue '%s' failed: %s",
                    consumer,
                    connection.getConnectionName(),
                    registerException.getMessage()
            ));
        }
    }
}
