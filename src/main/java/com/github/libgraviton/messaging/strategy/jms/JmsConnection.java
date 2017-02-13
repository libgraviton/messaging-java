package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.QueueConnection;
import com.github.libgraviton.messaging.exception.CannotCloseConnection;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import javax.jms.*;
import java.nio.charset.StandardCharsets;

/**
 * Represents a connection to a queue of a JMS compatible queue system. In case of a queue com.github.libgraviton.messaging.exception, the connection
 * will try to recover itself.
 */
public class JmsConnection extends QueueConnection {

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private Session session;

    private String messageSelector;

    protected Queue queue;

    protected String queueName;

    /**
     * Creates a JMS queue connection.
     *
     * @param queueName The name of the queue
     * @param connectionFactory The JMS Connection factory
     */
    public JmsConnection(String queueName, ConnectionFactory connectionFactory) {
        this.queueName = queueName;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String getConnectionName() {
        return queueName;
    }

    /**
     * Whtether the connection is open or not. Since JMS does not seem to provide such a functionality, this method
     * just checks whether a {@link Connection} has been instantiatet or not.
     *
     * @return true if the connection is open, false if not.
     */
    @Override
    public boolean isOpen() {
        return null != connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public Session getSession() {
        return session;
    }

    public Queue getQueue() {
        return queue;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    /**
     * Defines the JMS Message selector for consumers.
     *
     * @see Session#createConsumer(Destination, String)
     *
     * @param messageSelector The JMS message selector
     */
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    /**
     * Opens the connection by creating a new {@link Connection}, {@link Session} and {@link Queue}.
     *
     * @throws CannotConnectToQueue If the connection cannot be established
     */
    @Override
    protected void openConnection() throws CannotConnectToQueue {
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE); // AutoAck is done by JmsConsumer
            queue = session.createQueue(queueName);
            connection.setExceptionListener(new RecoveringExceptionListener(this));
        } catch (JMSException e) {
            throw new CannotConnectToQueue(queueName, e);
        }
    }

    /**
     * Registers a {@link MessageConsumer} with {@link MessageListener}.
     *
     * @param consumer The consumer to register. All messages will be acknowledged automatically. Except if the consumer
     *                 implements {@link AcknowledgingConsumer}.
     *
     * @throws CannotRegisterConsumer If the consumer cannot be registerd
     */
    @Override
    protected void registerConsumer(Consumer consumer) throws CannotRegisterConsumer {
        MessageListener jmsConsumer = new JmsConsumer(consumer);
        MessageConsumer messageConsumer;
        try {
            connection.setExceptionListener(new ReRegisteringExceptionListener(this, consumer));
            if (null != messageSelector) {
                messageConsumer = session.createConsumer(queue, messageSelector);
            } else {
                messageConsumer = session.createConsumer(queue);
            }
            messageConsumer.setMessageListener(jmsConsumer);
            connection.start();
        } catch (JMSException e) {
            throw new CannotRegisterConsumer(consumer, e);
        }
    }

    /**
     * Publishes a {@link BytesMessage}. Note that every message is considered UTF-8 encoded.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    @Override
    protected void publishMessage(String message) throws CannotPublishMessage {
        try {
            MessageProducer producer = session.createProducer(queue);
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeBytes(message.getBytes(StandardCharsets.UTF_8));
            producer.send(bytesMessage);
        } catch (JMSException e) {
            throw new CannotPublishMessage(message, e);
        }
    }

    /**
     * Closes the connection by closing the {@link Session} and {@link Connection}.
     *
     * @throws CannotCloseConnection If the connection cannot be closed.
     */
    @Override
    protected void closeConnection() throws CannotCloseConnection {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            throw new CannotCloseConnection(queueName, e);
        } finally {
            session = null;
            connection = null;
            queue = null;
        }
    }
}
