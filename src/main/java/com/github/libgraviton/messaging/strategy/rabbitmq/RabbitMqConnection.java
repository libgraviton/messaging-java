package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.QueueConnection;
import com.github.libgraviton.messaging.config.PropertyUtil;
import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.CannotCloseConnection;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Represents a connection to a RabbitMQ queue. In case of an com.github.libgraviton.messaging.exception on the queue or the channel, the connection will
 * try to recover itself.
 */
public class RabbitMqConnection extends QueueConnection {

    private static final Map<String, Object> QUEUE_ARGS = null;

    private final  boolean queueDurable;

    private final boolean queueExclusive;

    private final boolean queueAutoDelete;

    private final String exchangeName;

    private final boolean exchangeDurable;

    private final String exchangeType;

    private final String routingKey;

    private final ConnectionFactory connectionFactory;

    private String queueName;

    private Connection connection;

    private Channel channel;

    private RabbitMqConnection(Builder builder) {
        super(builder);
        queueDurable = builder.queueDurable;
        queueExclusive = builder.queueExclusive;
        queueAutoDelete = builder.queueAutoDelete;
        exchangeName = builder.exchangeName;
        exchangeDurable = builder.exchangeDurable;
        exchangeType = builder.exchangeType;
        routingKey = builder.routingKey;
        connectionFactory = builder.connectionFactory;
        queueName = super.queueName;
    }

    @Override
    public String getConnectionName() {
        return String.format(
                "%s - %s",
                null == exchangeName ? "default-exchange" : exchangeName,
                null == queueName ? "temporary-queue" : queueName
        );
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
     * Opens the connection. If no exchangeName is defined, it will bind to the default exchangeName
     * of RabbitMQ. But note that you need to define an exchangeName in order to publish messages.
     *
     * @see Builder#exchangeName(String)
     *
     * @throws CannotConnectToQueue If the connection cannot be established
     */
    @Override
    protected void openConnection() throws CannotConnectToQueue {
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            // If defined, use specific queue and declare it, otherwise use random / temporary queue
            if (null != queueName) {
                channel.queueDeclare(
                        queueName,
                        queueDurable,
                        queueExclusive,
                        queueAutoDelete,
                        QUEUE_ARGS
                );
            } else {
                queueName = channel.queueDeclare().getQueue();
            }
            // If defined, use specific exchange and bind queue to it, otherwise use default exchange
            if (null != exchangeName) {
                channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable);
                channel.queueBind(queueName, exchangeName, routingKey);
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
     * Publishes a text message on the queue. Note that this method uses UTF-8 encoding only.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    @Override
    protected void publishMessage(String message) throws CannotPublishMessage {
        publishMessage(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Publishes a bytes message on the queue.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    @Override
    protected void publishMessage(byte[] message) throws CannotPublishMessage {
        try {
            channel.basicPublish(
                    exchangeName,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message
            );
        } catch (IOException e) {
            throw new CannotPublishMessage(new String(message), e);
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

    Channel getChannel() {
        return channel;
    }

    /**
     * Builder class for creating RabbitMQ connections.
     */
    public static class Builder extends QueueConnection.Builder<Builder> {

        static final private boolean AUTO_RECOVERY = true;

        static final private ExceptionHandler EXCEPTION_HANDLER = new QueueExceptionLogger();

        private boolean queueDurable = true;

        private boolean queueExclusive = false;

        private boolean queueAutoDelete = false;

        private String exchangeName = null;

        private boolean exchangeDurable = false;

        private String exchangeType = "direct";

        private String routingKey = null;

        private String virtualHost = "/";

        private ConnectionFactory connectionFactory;

        /**
         * Overrides some defaults.
         */
        public Builder() {
            port(5672).user("guest").password("guest");
        }

        /**
         * Defines the durability of the queue. Default is true. Only applies if a queue is specified.
         *
         * @see #queueName(String)
         *
         * @param queueDurable The queue's durability
         *
         * @return self
         */
        public Builder queueDurable(boolean queueDurable) {
            this.queueDurable = queueDurable;
            return this;
        }

        /**
         * Defines the exclusivity of the queue. Default is false. Only applies if a queue is specified.
         *
         * @see #queueName(String)
         *
         * @param queueExclusive The queue's exclusivity
         *
         * @return self
         */
        public Builder queueExclusive(boolean queueExclusive) {
            this.queueExclusive = queueExclusive;
            return this;
        }

        /**
         * Defines whether the queue should get automatically deleted. Default is false. Only applies if a queue is
         * specified.
         *
         * @see #queueName(String)
         *
         * @param queueAutoDelete Whether the queue should get automatically deleted
         *
         * @return self
         */
        public Builder queueAutoDelete(boolean queueAutoDelete) {
            this.queueAutoDelete = queueAutoDelete;
            return this;
        }

        /**
         * Sets the exchange name. Default is the RabbitMQ default exchange.
         *
         * @param exchangeName The exchange name
         *
         * @return self
         */
        public Builder exchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
            return this;
        }

        /**
         * Defines the type of the exchange. Default is 'direct'. Only applies if an exchange is specified.
         *
         * @see #exchangeName(String)
         *
         * @param exchangeType The exchange's type
         *
         * @return self
         */
        public Builder exchangeType(String exchangeType) {
            this.exchangeType = exchangeType;
            return this;
        }

        /**
         * Defines the durability of the exchange. Default is true. Only applies if an exchange is specified.
         *
         * @see #exchangeName(String)
         *
         * @param exchangeDurable The exchange's durability
         *
         * @return self
         */
        public Builder exchangeDurable(boolean exchangeDurable) {
            this.exchangeDurable = exchangeDurable;
            return this;
        }

        /**
         * Sets the routing key.
         *
         * @param routingKey The routing key
         *
         * @return self
         */
        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        /**
         * Sets the RabbitMQ virtual host
         *
         * @param virtualHost The virtual host
         *
         * @return self
         */
        public Builder virtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
            return this;
        }

        Builder connectionFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        @Override
        public Builder applyProperties(Properties properties) {
            return super.applyProperties(properties)
                    .queueDurable(PropertyUtil.getBoolean(properties, "queue.durable", queueDurable))
                    .queueExclusive(PropertyUtil.getBoolean(properties, "queue.exclusive", queueExclusive))
                    .queueAutoDelete(PropertyUtil.getBoolean(properties, "queue.autodelete", queueAutoDelete))
                    .exchangeName(properties.getProperty("exchange.name", exchangeName))
                    .exchangeType(properties.getProperty("exchange.type", exchangeType))
                    .exchangeDurable(PropertyUtil.getBoolean(properties, "exchange.durable", exchangeDurable))
                    .routingKey(properties.getProperty("routingkey", routingKey))
                    .virtualHost(properties.getProperty("virtualhost", virtualHost));
        }

        @Override
        public RabbitMqConnection build() {
            if (null == connectionFactory) {
                connectionFactory = new ConnectionFactory();
                connectionFactory.setHost(host);
                connectionFactory.setPort(port);
                connectionFactory.setUsername(user);
                connectionFactory.setPassword(password);
                connectionFactory.setVirtualHost(virtualHost);
                connectionFactory.setExceptionHandler(EXCEPTION_HANDLER);
                connectionFactory.setAutomaticRecoveryEnabled(AUTO_RECOVERY);
            }
            return new RabbitMqConnection(this);
        }

    }

}
