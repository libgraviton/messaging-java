package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.QueueConnection;
import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.*;

import javax.jms.*;
import java.util.Properties;

/**
 * Represents a connection to a queue of a JMS compatible queue system. In case of a queue com.github.libgraviton.messaging.exception, the connection
 * will try to recover itself.
 */
public class JmsConnection extends QueueConnection {

    private final ConnectionFactory connectionFactory;

    private final String messageSelector;

    private Connection connection;

    private Session session;

    private Queue queue;

    /**
     * Creates a JMS queue connection.
     *
     * @param builder The connection builder
     */
    protected JmsConnection(JmsConnection.Builder builder) {
        super(builder);
        connectionFactory = builder.connectionFactory;
        messageSelector = builder.messageSelector;
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
     * Publishes a {@link TextMessage}. Note that every message is considered UTF-8 encoded.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    @Override
    protected void publishMessage(String message) throws CannotPublishMessage {
        try {
            MessageProducer producer = session.createProducer(queue);
            TextMessage textMessage = session.createTextMessage(message);
            producer.send(textMessage);
        } catch (JMSException e) {
            throw new CannotPublishMessage(message, e);
        }
    }

    @Override
    protected void publishMessage(byte[] message) throws CannotPublishMessage {
        try {
            MessageProducer producer = session.createProducer(queue);
            BytesMessage bytesMessage = session.createBytesMessage();
            producer.send(bytesMessage);
        } catch (JMSException e) {
            throw new CannotPublishMessage(new String(message), e);
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

    public static class Builder<JmsBuilder extends Builder> extends QueueConnection.Builder<JmsBuilder> {

        protected ConnectionFactory connectionFactory;

        protected String messageSelector;

        /**
         * Apply defaults
         */
        public Builder() {
            port(61616);
        }

        /**
         * Defines the JMS connection factory.
         *
         * @param connectionFactory The connection factory
         *
         * @return self
         */
        public JmsBuilder connectionFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return (JmsBuilder) this;
        }

        /**
         * Defines the JMS Message selector for consumers.
         *
         * @see Session#createConsumer(Destination, String)
         *
         * @param messageSelector The JMS message selector
         *
         * @return self
         */
        public JmsBuilder messageSelector(String messageSelector) {
            this.messageSelector = messageSelector;
            return (JmsBuilder) this;
        }

        @Override
        public JmsBuilder applyProperties(Properties properties) {
            super.applyProperties(properties)
                    .messageSelector(properties.getProperty("message.selector", messageSelector));
            return (JmsBuilder) this;
        }

        @Override
        public JmsConnection build() throws CannotBuildConnection {
            return new JmsConnection(this);
        }
    }
}
