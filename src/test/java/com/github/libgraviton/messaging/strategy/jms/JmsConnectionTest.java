package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import javax.jms.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JmsConnectionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private JmsConnection connection;

    private Queue jmsQueue;

    private Session jmsSession;

    private Connection jmsConnection;

    private ConnectionFactory jmsFactory;

    @Before
    public void setUp() throws Exception{
        jmsQueue = mock(Queue.class);

        jmsSession = mock(Session.class);
        doReturn(jmsQueue).when(jmsSession).createQueue("queue");

        jmsConnection = mock(Connection.class);
        doReturn(jmsSession).when(jmsConnection).createSession(false, Session.CLIENT_ACKNOWLEDGE);

        jmsFactory = mock(ConnectionFactory.class);
        doReturn(jmsConnection).when(jmsFactory).createConnection();
        connection = (JmsConnection) new JmsConnection.Builder<>()
                .connectionAttempts(1)
                .connectionFactory(jmsFactory)
                .queueName("queue")
                .build();
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
        assertEquals("queue", connection.getConnectionName());
    }

    @Test
    public void testOpenConnection() throws Exception {
        connection.open();
        verify(jmsConnection).createSession(false, Session.CLIENT_ACKNOWLEDGE);
        verify(jmsConnection).setExceptionListener(any(RecoveringExceptionListener.class));
        verify(jmsSession).createQueue("queue");
    }

    @Test
    public void testOpenConnectionFailed() throws Exception {
        thrown.expect(CannotConnectToQueue.class);

        doThrow(new JMSException("gugus")).when(jmsFactory).createConnection();
        connection.open();
    }

    @Test
    public void testRegisterConsumer() throws Exception {
        MessageConsumer jmsConsumer = mock(MessageConsumer.class);
        doReturn(jmsConsumer).when(jmsSession).createConsumer(jmsQueue);

        Consumer consumer = mock(Consumer.class);
        connection.consume(consumer);
        verify(jmsSession).createConsumer(jmsQueue);
        verify(jmsSession, never()).createConsumer(eq(jmsQueue), anyString());
        verify(jmsConsumer).setMessageListener(any(JmsConsumer.class));
        verify(jmsConnection).start();

        // Normal recovering exception listener first, re-registering exception listener second
        verify(jmsConnection, times(2)).setExceptionListener(any(ExceptionListener.class));
    }

    @Test
    public void testRegisterConsumerWithMessageSelector() throws Exception {
        MessageConsumer jmsConsumer = mock(MessageConsumer.class);
        doReturn(jmsConsumer).when(jmsSession).createConsumer(eq(jmsQueue), anyString());

        Consumer consumer = mock(Consumer.class);

        connection = (JmsConnection) new JmsConnection.Builder<>()
                .connectionAttempts(1)
                .connectionFactory(jmsFactory)
                .messageSelector("selector")
                .queueName("queue")
                .build();

        connection.consume(consumer);

        verify(jmsSession, never()).createConsumer(jmsQueue);
        verify(jmsSession).createConsumer(jmsQueue, "selector");
    }

    @Test
    public void testRegisterConsumerFailed() throws Exception {
        thrown.expect(CannotRegisterConsumer.class);

        doThrow(new JMSException("gugus")).when(jmsSession).createConsumer(jmsQueue);
        connection.consume(mock(Consumer.class));
    }

    @Test
    public void testPublishMessage() throws Exception {
        MessageProducer jmsProducer = mock(MessageProducer.class);

        doReturn(mock(BytesMessage.class)).when(jmsSession).createBytesMessage();
        doReturn(jmsProducer).when(jmsSession).createProducer(jmsQueue);
        connection.publish("gugus");
        verify(jmsProducer).send(any(BytesMessage.class));
    }

    @Test
    public void testPublishMessageFailed() throws Exception {
        thrown.expect(CannotPublishMessage.class);

        doThrow(new JMSException("gugus")).when(jmsSession).createProducer(jmsQueue);
        connection.publish("gugus");
    }

    @Test
    public void testCloseConnection() throws Exception {
        connection.open();
        verify(jmsSession, never()).close();
        verify(jmsConnection, never()).close();
        connection.close();
        verify(jmsSession).close();
        verify(jmsConnection).close();
    }

    @Test
    public void testCloseConnectionFailed() throws Exception {
        doThrow(new JMSException("gugus")).when(jmsSession).close();

        connection.open();
        connection.close();

        // Expects no com.github.libgraviton.messaging.exception. If any is thrown, this test would fail.
    }
}
