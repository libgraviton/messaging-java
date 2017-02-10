package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.exception.CannotConsumeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelConsumer implements Consumer {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    private Consumer consumer;

    public ParallelConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void consume(String messageId, String message) {
        new Thread(new ConsumerRunner(consumer, messageId, message)).start();
    }

    private class ConsumerRunner implements Runnable {

        private Consumer consumer;

        private String messageId;

        private String message;

        ConsumerRunner(Consumer consumer, String messageId, String message) {
            this.consumer = consumer;
            this.messageId = messageId;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                consumer.consume(messageId, message);
            } catch (CannotConsumeMessage e) {
                LOG.warn(String.format("Consumer '%s' failed: '%s'", consumer, e.getMessage()));
            }
        }
    }

}
