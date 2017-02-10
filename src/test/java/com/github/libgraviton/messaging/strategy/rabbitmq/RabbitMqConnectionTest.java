package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RabbitMqConnectionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RabbitMqConnection connection;

    private Channel rabbitChannel;

    private Connection rabbitConnection;

    private ConnectionFactory rabbitFactory;

    @Before
    public void setUp() throws Exception{
        rabbitChannel = mock(Channel.class);

        rabbitConnection = mock(Connection.class);
        doReturn(rabbitChannel).when(rabbitConnection).createChannel();
        doReturn(true).when(rabbitChannel).isOpen();
        doReturn(true).when(rabbitConnection).isOpen();

        rabbitFactory = mock(ConnectionFactory.class);
        doReturn(rabbitConnection).when(rabbitFactory).newConnection();
        connection = new RabbitMqConnection("queue", "exchange", "routingKey", rabbitFactory);
        connection.setConnectionAttempts(1);
    }

    @After
    public void tearDown() {
        connection.close();
    }

    @Test
    public void testIsOpen() throws Exception {
        assertFalse(connection.isOpen());
        connection.open();
        assertTrue(connection.isOpen());
        connection.close();
        assertFalse(connection.isOpen());
    }

    @Test
    public void testQueuName() {
        assertEquals("queue", connection.getQueueName());
    }

    @Test
    public void testDeclareChannelDefaults() throws Exception {
        connection.open();
        verify(rabbitChannel).queueDeclare("queue", true, false, false, null);
    }

    @Test
    public void testDeclareChannelCustom() throws Exception {
        final boolean autoAck = true;
        final boolean durable= false;
        final boolean exclusive = true;

        connection.setQueueAutoDelete(autoAck);
        connection.setQueueDurable(durable);
        connection.setQueueExclusive(exclusive);
        connection.open();
        verify(rabbitChannel).queueDeclare("queue", durable, exclusive, autoAck, null);
    }

    @Test
    public void testDecalreExchangeDefaults() throws Exception {
        connection.open();
        verify(rabbitChannel).exchangeDeclare("exchange", "direct", false);
        verify(rabbitChannel).queueBind("queue", "exchange", "routingKey");
    }

    @Test
    public void testDecalreExchangeCustom() throws Exception {
        connection.setExchangeType("topic");
        connection.setExchangeDurable(true);
        connection.open();
        verify(rabbitChannel).exchangeDeclare("exchange", "topic", true);
        verify(rabbitChannel).queueBind("queue", "exchange", "routingKey");
    }

    @Test
    public void testConnectDefaultExchange() throws Exception {
        connection = new RabbitMqConnection("queue", null, null, rabbitFactory);
        connection.open();
        verify(rabbitChannel).queueDeclare("queue", true, false, false, null);
        verify(rabbitChannel, never()).exchangeDeclare(anyString(), anyString(), anyBoolean());
        verify(rabbitChannel, never()).queueBind(anyString(), anyString(), anyString());
    }

    @Test
    public void testOpenConnectionFailed() throws Exception {
        thrown.expect(CannotConnectToQueue.class);

        doThrow(new IOException()).when(rabbitFactory).newConnection();
        connection.open();
    }

    @Test
    public void testRegisterConsumer() throws Exception {
        Consumer consumer = mock(Consumer.class);
        connection.consume(consumer);
        verify(rabbitChannel).basicConsume(eq("queue"), eq(true), any(RabbitMqConsumer.class));
    }

    @Test
    public void testRegisterAcknowledgingConsumer() throws Exception {
        Consumer consumer = mock(AcknowledgingConsumer.class);
        connection.consume(consumer);
        verify(rabbitChannel).basicConsume(eq("queue"), eq(false), any(RabbitMqConsumer.class));
    }

    @Test
    public void testRegisterConsumerFailed() throws Exception {
        thrown.expect(CannotRegisterConsumer.class);

        doThrow(new IOException()).when(rabbitChannel).basicConsume(eq("queue"), eq(true), any(RabbitMqConsumer.class));
        Consumer consumer = mock(Consumer.class);
        connection.consume(consumer);
    }

    @Test
    public void testPublishMessage() throws Exception {
        connection.publish("gugus");
        verify(rabbitChannel).basicPublish(
                "exchange",
                "routingKey",
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                "gugus".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    public void testPublishMessageFailed() throws Exception {
        thrown.expect(CannotPublishMessage.class);

        doThrow(new IOException()).when(rabbitChannel).basicPublish(
                "exchange",
                "routingKey",
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                "gugus".getBytes(StandardCharsets.UTF_8)
        );
        connection.publish("gugus");
    }

    @Test
    public void testCloseConnection() throws Exception {
        connection.open();
        verify(rabbitChannel, never()).close();
        verify(rabbitConnection, never()).close();
        connection.close();
        verify(rabbitChannel).close();
        verify(rabbitConnection).close();
    }

    @Test
    public void testCloseConnectionFailed() throws Exception {
        doThrow(new IOException()).when(rabbitChannel).close();

        connection.open();
        connection.close();

        // Expects no com.github.libgraviton.messaging.exception. If any is thrown, this test would fail.
    }
}
