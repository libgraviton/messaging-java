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

        connection = new RabbitMqConnection.Builder()
                .queueName("queue")
                .exchangeName("exchange")
                .routingKey("routingKey")
                .build();
        connection.setConnectionAttempts(1);
        connection = spy(connection);

        doReturn(rabbitFactory).when(connection).createConnectionFactory();
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
    public void testDefaultConfig() throws Exception {
        final boolean queueDurable= true;
        final boolean queueExclusive = false;
        final boolean queueAutoDelete = false;
        final boolean exchangeDurable = false;

        connection.open();

        verify(rabbitChannel).queueDeclare("queue", queueDurable, queueExclusive, queueAutoDelete, null);
        verify(rabbitChannel).exchangeDeclare("exchange", "direct", exchangeDurable);
        verify(rabbitChannel).queueBind("queue", "exchange", "routingKey");
    }

    @Test
    public void testCustomConfig() throws Exception {
        final boolean queueDurable= false;
        final boolean queueExclusive = true;
        final boolean queueAutoDelete = true;
        final boolean exchangeDurable = true;

        connection = new RabbitMqConnection.Builder()
                .queueName("custom-queue")
                .queueAutoDelete(queueAutoDelete)
                .queueDurable(queueDurable)
                .queueExclusive(queueExclusive)
                .exchangeName("custom-exchange")
                .exchangeType("topic")
                .exchangeDurable(exchangeDurable)
                .routingKey("custom-routing-key")
                .build();

        connection = spy(connection);
        doReturn(rabbitFactory).when(connection).createConnectionFactory();

        connection.open();
        verify(rabbitChannel).queueDeclare("custom-queue", queueDurable, queueExclusive, queueAutoDelete, null);
        verify(rabbitChannel).exchangeDeclare("custom-exchange", "topic", exchangeDurable);
        verify(rabbitChannel).queueBind("custom-queue", "custom-exchange", "custom-routing-key");
    }
    
    @Test
    public void testConnectDefaultExchange() throws Exception {
        connection = spy(new RabbitMqConnection.Builder().queueName("queue").exchangeName(null).build());
        doReturn(rabbitFactory).when(connection).createConnectionFactory();

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
