package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotAcknowledgeMessage;
import com.github.libgraviton.messaging.exception.CannotConsumeMessage;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import com.github.libgraviton.messaging.strategy.rabbitmq.RabbitMqConnection;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Ensures that the examples in the README are valid.
 *
 * IF YOU NEED TO CHANGE ANYTHING HERE, YOU NEED TO UPDATE /README.md ACCORDINGLY.
 */
public class ReadmeExamplesTest {

    QueueConnection connection;

    @Before
    public void setUp() throws Exception {
        connection = mock(QueueConnection.class, CALLS_REAL_METHODS);
        doNothing().when(connection).publishMessage(anyString());
        doNothing().when(connection).registerConsumer(any(Consumer.class));
    }

    @Test
    public void testPublishExample() {
        // QueueConnection connection = new RabbitMqConnection.Builder().queueName("your-queue").build();

        try {
            connection.publish("the message");
        } catch (CannotPublishMessage e) {
            // Message publishment failed for some reason.
            fail(String.format("An exception occurred: '%s'", e.getClass().getName()));
        }
    }

    @Test
    public void testConsumeExample() {
        Consumer consumer = new Consumer() {

            @Override
            public void consume(String messageId, String message) throws CannotConsumeMessage {
                System.out.println(String.format("Received message with id '%s': '%s'", messageId, message));
            }

        };

        // QueueConnection connection = new RabbitMqConnection.Builder().queueName("your-queue").build();

        try {
            connection.consume(consumer);
        } catch (CannotRegisterConsumer e) {
            // Consumer registration failed for some reason.
            fail(String.format("An exception occurred: '%s'", e.getClass().getName()));
        }
    }

    @Test
    public void testAcknoledgingConsumeExample() {
        Consumer consumer = new AcknowledgingConsumer() {

            private MessageAcknowledger acknowledger;

            @Override
            public void setAcknowledger(MessageAcknowledger acknowledger) {
                this.acknowledger = acknowledger;
            }

            @Override
            public void consume(String messageId, String message) throws CannotConsumeMessage {
                System.out.println(String.format("Received message with id '%s': '%s'", messageId, message));
                try {
                    acknowledger.acknowledge(messageId);
                } catch (CannotAcknowledgeMessage e) {
                    // Message Acknowledgment failed for some reason
                    throw new CannotConsumeMessage(messageId, message, e);
                }
            }

        };

        // QueueConnection connection = new RabbitMqConnection.Builder().queueName("your-queue").build();

        try {
            connection.consume(consumer);
        } catch (CannotRegisterConsumer e) {
            // Consumer registration failed for some reason.
            fail(String.format("An exception occurred: '%s'", e.getClass().getName()));
        }
    }

}
