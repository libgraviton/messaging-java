package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.MessageAcknowledger;
import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotAcknowledgeMessage;
import com.github.libgraviton.messaging.exception.CannotConsumeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.nio.charset.StandardCharsets;

/**
 * Wraps an instance of {@link Consumer} in order to consume from a JMS based queue.
 *
 * Moreover, this class does also the automatic message acknowledgment after the
 * {@link Consumer#consume(String, String)} terminated. Except if the wrapped {@link Consumer} is an
 * {@link AcknowledgingConsumer}, it will do the JMS acknowledgment as soon as it receives the acknowledgment from the
 * wrapped consumer.
 */
class JmsConsumer implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(JmsConsumer.class);
    private Consumer consumer;

    JmsConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessage(Message jmsMessage) {
        LOG.debug(String.format("Received message of type '%s' from queue.", jmsMessage.getClass().getName()));
        String message;
        String messageId = null;

        try {
            messageId = jmsMessage.getJMSMessageID();

            if (jmsMessage instanceof TextMessage) {
                message = ((TextMessage) jmsMessage).getText();
            } else if (jmsMessage instanceof BytesMessage) {
                message = extractBody((BytesMessage) jmsMessage);
            } else {
                LOG.warn(String.format(
                        "Message of type '%s' cannot be handled and got ignored.",
                        jmsMessage.getClass().getName()
                ));
                return;
            }

            consumer.consume(messageId, message);
        } catch (JMSException | CannotConsumeMessage e) {
            LOG.error("Could not process feedback message.", e);
        } catch (Exception e) {
            // Catch com.github.libgraviton.messaging.exception to avoid endless loop because the message will trigger 'onMessage' again and again.
            LOG.error("Unexpected error occurred while processing queue feedback message.", e);
        } finally {
            if (!(consumer instanceof AcknowledgingConsumer)) {
                acknowledge(jmsMessage,messageId);
            }
        }
    }

    private void acknowledge(Message jmsMessage,String messageId) {
        try {
            jmsMessage.acknowledge();
        } catch (JMSException e) {
            try {
                throw new CannotAcknowledgeMessage((MessageAcknowledger)this, messageId, e);
            } catch (CannotAcknowledgeMessage cam) {
                LOG.error(cam.getMessage());
            }
        }
    }

    String extractBody(BytesMessage message) throws JMSException {
        byte[] messageBytes = new byte[(int) message.getBodyLength()];
        message.readBytes(messageBytes);
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
}
