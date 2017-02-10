package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.consumer.AcknowledgingConsumer;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.QueueConnection;
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

    private Connection connection;

    private Channel channel;

    private Config config;

    private RabbitMqConnection(Config config) {
        super(config.queueName);
        this.config = config;
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
     * Opens the connection. If no config.getExchangeName() is defined, it will bind to the default config.getExchangeName() of RabbitMQ. But note that
     * you need to define an config.getExchangeName() in order to publish messages.
     *
     * @throws CannotConnectToQueue If the connection cannot be established
     */
    @Override
    protected void openConnection() throws CannotConnectToQueue {
        try {
            connection = createConnectionFactory().newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(
                    config.queueName,
                    config.queueDurable,
                    config.queueExclusive,
                    config.queueAutoDelete,
                    config.QUEUE_ARGS
            );
            // If defined, use specific exchange and bind queue to it, otherwise use default exchange
            if (null != config.exchangeName) {
                channel.exchangeDeclare(config.exchangeName, config.exchangeType, config.exchangeDurable);
                channel.queueBind(config.queueName, config.exchangeName, config.routingKey);
            }
        } catch (IOException | TimeoutException e) {
            throw new CannotConnectToQueue(config.queueName, e);
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
            channel.basicConsume(config.queueName, autoAck, rabbitMqConsumer);
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
                    config.exchangeName,
                    config.routingKey,
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
            throw new CannotCloseConnection(config.queueName, e);
        } finally {
            connection = null;
            channel = null;
        }
    }

    Channel getChannel() {
        return channel;
    }

    ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.host);
        factory.setPort(config.port);
        factory.setUsername(config.user);
        factory.setPassword(config.password);
        factory.setExceptionHandler(config.EXCEPTION_HANDLER);
        factory.setAutomaticRecoveryEnabled(config.AUTO_RECOVERY);
        return factory;
    }

    private static class Config {

        private final Map<String, Object> QUEUE_ARGS = null;

        private final boolean AUTO_RECOVERY = true;

        private final ExceptionHandler EXCEPTION_HANDLER = new QueueExceptionLogger();

        private String queueName = "queue";

        private String host = "localhost";

        private int port = 5672;

        private String user = "guest";

        private String password = "guest";

        private boolean queueDurable = true;

        private boolean queueExclusive = false;

        private boolean queueAutoDelete = false;

        private String exchangeName = null;

        private boolean exchangeDurable = false;

        private String exchangeType = "direct";

        private String routingKey;

    }

    public static class Builder {

        private Config config;

        public Builder() {
            config = new Config();
        }

        public Builder(Properties properties) {
            this();
            host(properties.getProperty("host", config.host))
                    .port(Integer.parseInt(properties.getProperty("port", Integer.toString(config.port))))
                    .user(properties.getProperty("user", config.user))
                    .password(properties.getProperty("password", config.password))
                    .queueName(properties.getProperty("queue.name", config.queueName))
                    .queueDurable(booleanProperty(properties, "queue.durable", config.queueDurable))
                    .queueExclusive(booleanProperty(properties, "queue.exclusive", config.queueExclusive))
                    .queueAutoDelete(booleanProperty(properties, "queue.autodelete", config.queueAutoDelete))
                    .exchangeName(properties.getProperty("exchange.name", config.exchangeName))
                    .exchangeType(properties.getProperty("exchange.type", config.exchangeType))
                    .exchangeDurable(booleanProperty(properties, "exchange.durable", config.exchangeDurable))
                    .routingKey(properties.getProperty("routingkey"));
        }

        public RabbitMqConnection build() {
            return new RabbitMqConnection(config);
        }

        public Builder queueName(String queueName) {
            config.queueName = queueName;
            return this;
        }

        public Builder queueDurable(boolean queueDurable) {
            config.queueDurable = queueDurable;
            return this;
        }

        public Builder queueExclusive(boolean queueExclusive) {
            config.queueExclusive = queueExclusive;
            return this;
        }

        public Builder queueAutoDelete(boolean queueAutoDelete) {
            config.queueAutoDelete = queueAutoDelete;
            return this;
        }

        public Builder exchangeName(String exchangeName) {
            config.exchangeName = exchangeName;
            return this;
        }

        public Builder exchangeType(String exchangeType) {
            config.exchangeType = exchangeType;
            return this;
        }

        public Builder exchangeDurable(boolean exchangeDurable) {
            config.exchangeDurable = exchangeDurable;
            return this;
        }

        public Builder routingKey(String routingKey) {
            config.routingKey = routingKey;
            return this;
        }

        public Builder host(String host) {
            config.host = host;
            return this;
        }

        public Builder port(int port) {
            config.port = port;
            return this;
        }

        public Builder user(String user) {
            config.user = user;
            return this;
        }

        public Builder password(String password) {
            config.password = password;
            return  this;
        }

        private boolean booleanProperty(Properties properties, String name, boolean defaultValue) {
            return Boolean.valueOf(properties.getProperty(name, Boolean.toString(defaultValue)));
        }

    }

}
