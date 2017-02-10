package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotCloseConnection;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QueueConnectionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private QueueConnection connection;

    @Before
    public void setUp() throws Exception{
        connection = mock(QueueConnection.class, CALLS_REAL_METHODS);
        connection.setConnectionAttempts(1);
        doNothing().when(connection).openConnection();
        doNothing().when(connection).closeConnection();
        doNothing().when(connection).registerConsumer(any(Consumer.class));
        doNothing().when(connection).publishMessage(anyString());
    }

    @Test
    public void tesOpenConnection() throws Exception {
        connection.open();
        verify(connection).openConnection();
    }

    @Test
    public void tesOpenConnectionFailed() throws Exception {
        thrown.expect(CannotConnectToQueue.class);

        doThrow(new CannotConnectToQueue("gugus", new Exception())).when(connection).openConnection();
        connection.open();
    }

    @Test
    public void testOpenConnectionRetry() throws Exception {
        doThrow(new CannotConnectToQueue("gugus", null)).when(connection).openConnection();

        connection.setConnectionAttempts(5);
        connection.setConnectionAttemptSleep(0.001); // Save time (sleep for 1ms)

        boolean exceptionThrown = false;
        try {
            connection.open();
        } catch (CannotConnectToQueue e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        verify(connection, times(5)).openConnection();
    }

    @Test
    public void testOpenIfClosed() throws Exception {
        connection.open();
        doReturn(true).when(connection).isOpen();
        assertFalse(connection.openIfClosed());
        verify(connection, times(1)).open();
        verify(connection, never()).close();
    }

    @Test
    public void testCloseConnection() throws Exception{
        connection.close();
        verify(connection).closeConnection();
    }

    @Test
    public void tesCloseConnectionFailed() throws Exception {
        doThrow(new CannotCloseConnection("gugus", new Exception())).when(connection).closeConnection();
        connection.open();
        connection.close();

        // Expects no com.github.libgraviton.messaging.exception. If any is thrown, this test would fail.
    }

    @Test
    public void testPublishMessage() throws Exception {
        connection.publish("gugus");
        verify(connection).publishMessage("gugus");
        assertFalse(connection.isOpen());
    }

    @Test
    public void testPublishMessageAlreadyOpen() throws Exception {
        doReturn(true).when(connection).isOpen();

        connection.publish("gugus");

        verify(connection, never()).open();
        verify(connection, never()).close();
    }

    @Test
    public void testPublishMessageFailed() throws Exception {
        thrown.expect(CannotPublishMessage.class);

        doThrow(new CannotPublishMessage("gugus", new Exception())).when(connection).publishMessage("gugus");
        connection.publish("gugus");
    }

    @Test
    public void testRegisterConsumer() throws Exception{
        Consumer consumer = mock(Consumer.class);
        connection.consume(consumer);
        verify(connection).registerConsumer(consumer);
    }

    @Test
    public void testRegisterConsumerFailed() throws Exception {
        thrown.expect(CannotRegisterConsumer.class);

        Consumer consumer = mock(Consumer.class);
        doThrow(new CannotRegisterConsumer(consumer, "gugus")).when(connection).registerConsumer(consumer);
        connection.consume(consumer);
    }

    @Test
    public void testRegisterSecondConsumer() throws Exception {
        thrown.expect(CannotRegisterConsumer.class);
        thrown.expectMessage("Another consumer is already registered.");

        connection.consume(mock(Consumer.class));
        connection.consume(mock(Consumer.class));
    }

}
