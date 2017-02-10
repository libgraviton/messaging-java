package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.AcknowledgingConsumer;
import com.github.libgraviton.messaging.Consumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

public class RabbitMqConsumerTest {

    private RabbitMqConsumer rabbitConsumer;

    private Consumer consumer;

    private RabbitMqConnection connection;

    private Envelope envelope;

    @Before
    public void setUp() throws Exception {
        connection = mock(RabbitMqConnection.class);
        consumer = mock(Consumer.class);
        rabbitConsumer = spy(new RabbitMqConsumer(connection, consumer));
        envelope = mock(Envelope.class);
        doReturn(1L).when(envelope).getDeliveryTag();
    }

    @Test
    public void testAutoAcknowledge() throws Exception {
        rabbitConsumer.handleDelivery(
                "consumrTag",
                envelope,
                mock(AMQP.BasicProperties.class),
                "message".getBytes(StandardCharsets.UTF_8)
        );
        verify(rabbitConsumer, never()).acknowledge(anyString());

        AcknowledgingConsumer acknowledgingConsumer = mock(AcknowledgingConsumer.class);
        rabbitConsumer = new RabbitMqConsumer(connection, acknowledgingConsumer);
        verify(acknowledgingConsumer).setAcknowledger(rabbitConsumer);
    }

    @Test
    public void testMessageDelegation() throws Exception {
        rabbitConsumer.handleDelivery(
                "consumerTag",
                envelope,
                mock(AMQP.BasicProperties.class),
                "message".getBytes(StandardCharsets.UTF_8)
        );

        verify(consumer).consume("1", "message");
    }

    @Test
    public void testConnectionRecovery() throws Exception {
        ShutdownSignalException sig = mock(ShutdownSignalException.class);
        doReturn(mock(Channel.class)).when(sig).getReference();

        rabbitConsumer.handleShutdownSignal("consumerTag", sig);
        verify(connection).close();
        verify(connection).consume(consumer);
    }

}
