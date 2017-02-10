package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.QueueConnection;
import com.github.libgraviton.messaging.exception.CannotCloseConnection;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Represents a connection to a RabbitMQ queue. In case of an com.github.libgraviton.messaging.exception on the queue or the channel, the connection will
 * try to recover itself.
 */
public class RabbitMqConnection extends QueueConnection {

    private final Map<String, Object> QUEUE_ARGS = null;

    private boolean queueDurable = true;

    private boolean queueExclusive = false;

    private boolean queueAutoDelete = false;

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private Channel channel;

    private String exchange;

    private boolean exchangeDurable = false;

    private String exchangeType = "direct";

    private String routingKey;

    /**
     * Creates a RabbitMQ queue connection.
     *
     * @param queueName The name of the queue
     * @param exchange The name of the exchange
     * @param routingKey The routing key
     * @param connectionFactory The RabbitMQ connection factory
     */
    public RabbitMqConnection(
            String queueName,
            String exchange,
            String routingKey,
            ConnectionFactory connectionFactory
    ) {
        super(queueName);
        this.connectionFactory = connectionFactory;
        this.exchange = exchange;
        this.routingKey = routingKey;
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setExceptionHandler(new QueueExceptionLogger());
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * Defines the durable parameter of {@link Channel#queueDeclare(String, boolean, boolean, boolean, Map)}.
     *
     * @param queueDurable Whether the queue is durable or not
     */
    public void setQueueDurable(boolean queueDurable) {
        this.queueDurable = queueDurable;
    }

    /**
     * Defines the exclusive parameter of {@link Channel#queueDeclare(String, boolean, boolean, boolean, Map)}.
     *
     * @param queueExclusive Whether the queue is exclusive or not
     */
    public void setQueueExclusive(boolean queueExclusive) {
        this.queueExclusive = queueExclusive;
    }

    /**
     * Defines the autoDelete parameter of {@link Channel#queueDeclare(String, boolean, boolean, boolean, Map)}.
     *
     * @param queueAutoDelete Whether the queue should be deleted automatically or not
     */
    public void setQueueAutoDelete(boolean queueAutoDelete) {
        this.queueAutoDelete = queueAutoDelete;
    }

    /**
     * Defines the durable parameter of {@link Channel#exchangeDeclare(String, String, boolean)}.
     *
     * @param exchangeDurable Whether the exchange is durable or not.
     */
    public void setExchangeDurable(boolean exchangeDurable) {
        this.exchangeDurable = exchangeDurable;
    }

    /**
     * Defines the type parameter of {@link Channel#exchangeDeclare(String, String, boolean)}.
     *
     * @param exchangeType The exchange type.
     */
    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }

    /**
     * Returns whether the connection is open by checking wheter the {@link Connection} or the {@link Channel} is open.
     *
     * @return true if the connection is open, otherwise false.
     */
    @Override
    public boolean isOpen() {
        return connection != null && connection.isOpen() || channel != null && channel.isOpen();
    }

    /**
     * Opens the connection. If no exchange is defined, it will bind to the default exchange of RabbitMQ. But note that
     * you need to define an exchange in order to publish messages.
     *
     * @throws CannotConnectToQueue If the connection cannot be established
     */
    @Override
    protected void openConnection() throws CannotConnectToQueue {
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, QUEUE_ARGS);
            // If defined, use specific exchange and bind queue to it, otherwise use default exchange
            if (null != exchange) {
                channel.exchangeDeclare(exchange, exchangeType, exchangeDurable);
                channel.queueBind(queueName, exchange, routingKey);
            }
        } catch (IOException | TimeoutException e) {
            throw new CannotConnectToQueue(queueName, e);
        }
    }

    /**
     * Registers a consumer. If the consumer implements {@link AcknowledgingConsumer}, the autoAck flag is set to false,
     * otherwise it's set to true.
     *
     * @param consumer The consumer to register.
     *
     * @throws CannotRegisterConsumer If the consumer cannot be registerd.
     */
    @Override
    protected void registerConsumer(Consumer consumer) throws CannotRegisterConsumer {
        RabbitMqConsumer rabbitMqConsumer = new RabbitMqConsumer(this, consumer);
        boolean autoAck = !(consumer instanceof AcknowledgingConsumer);
        if (!autoAck) {
            ((AcknowledgingConsumer) consumer).setAcknowledger(rabbitMqConsumer);
        }
        try {
            channel.basicConsume(queueName, autoAck, rabbitMqConsumer);
        } catch (IOException e) {
            throw new CannotRegisterConsumer(consumer, e);
        }
    }

    /**
     * Publishes a message on the queue. Note that this method uses UTF-8 encoding only.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    @Override
    protected void publishMessage(String message) throws CannotPublishMessage {
        try {
            channel.basicPublish(
                    exchange,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new CannotPublishMessage(message, e);
        }
    }

    /**
     * Closes the channel and the connection if they are open.
     *
     * @throws CannotCloseConnection If the channel and / or connection cannot be closed.
     */
    @Override
    protected void closeConnection() throws CannotCloseConnection {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            throw new CannotCloseConnection(queueName, e);
        } finally {
            connection = null;
            channel = null;
        }
    }
}
