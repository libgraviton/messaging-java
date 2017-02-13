package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.github.libgraviton.messaging.config.ContextProperties;
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
        this.config = config;
    }

    @Override
    public String getConnectionName() {
        return String.format(
                "%s - %s",
                null == config.exchangeName ? "default-exchange" : config.exchangeName,
                null == config.queueName ? "temporary-queue" : config.queueName
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
     * Opens the connection. If no config.exchangeName is defined, it will bind to the default config.exchangeName
     * of RabbitMQ. But note that you need to define an config.exchangeName in order to publish messages.
     *
     * @see Builder#exchangeName(String)
     *
     * @throws CannotConnectToQueue If the connection cannot be established
     */
    @Override
    protected void openConnection() throws CannotConnectToQueue {
        try {
            connection = createConnectionFactory().newConnection();
            channel = connection.createChannel();
            // If defined, use specific queue and declare it, otherwise use random / temporary queue
            if (null != config.queueName) {
                channel.queueDeclare(
                        config.queueName,
                        config.queueDurable,
                        config.queueExclusive,
                        config.queueAutoDelete,
                        config.QUEUE_ARGS
                );
            }
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

        private String queueName = null;

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

        private String routingKey = null;

    }

    /**
     * Builder class for creating RabbitMQ connections.
     */
    public static class Builder {

        private Config config;

        /**
         * Creates a new Builder by using all default values.
         */
        public Builder() {
            config = new Config();
        }


        /**
         * Creates a new Builder by using the property values in a given context of a given {@link Properties}.
         *
         * @param properties The properties instance
         * @param prefix The context (e.g. 'context' will lead to all matching properties under context.* being used)
         */
        public Builder(Properties properties, String prefix) {
            this(new ContextProperties(properties, prefix));
        }

        Builder(Properties properties) {
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
                    .routingKey(properties.getProperty("routingkey", config.routingKey));
        }

        /**
         * Builds the RabbitMQ Connection.
         *
         * @return The RabbitMQ Connection
         */
        public RabbitMqConnection build() {
            return new RabbitMqConnection(config);
        }

        /**
         * Sets the queue name. Default is a random queue.
         *
         * @param queueName The queue name
         *
         * @return self
         */
        public Builder queueName(String queueName) {
            config.queueName = queueName;
            return this;
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
            config.queueDurable = queueDurable;
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
            config.queueExclusive = queueExclusive;
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
            config.queueAutoDelete = queueAutoDelete;
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
            config.exchangeName = exchangeName;
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
            config.exchangeType = exchangeType;
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
            config.exchangeDurable = exchangeDurable;
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
            config.routingKey = routingKey;
            return this;
        }

        /**
         * Sets the host where RabbitMQ is accessible.
         *
         * @param host The host
         *
         * @return self
         */
        public Builder host(String host) {
            config.host = host;
            return this;
        }

        /**
         * Sets the port where RabbitMQ is accessible.
         *
         * @param port The port
         *
         * @return self
         */
        public Builder port(int port) {
            config.port = port;
            return this;
        }

        /**
         * Sets the user which will be used to establish the connection.
         *
         * @param user The user
         *
         * @return self
         */
        public Builder user(String user) {
            config.user = user;
            return this;
        }

        /**
         * Sets the password which will be used to establish the connection.
         *
         * @param password The password
         *
         * @return self
         */
        public Builder password(String password) {
            config.password = password;
            return  this;
        }

        private boolean booleanProperty(Properties properties, String name, boolean defaultValue) {
            return Boolean.valueOf(properties.getProperty(name, Boolean.toString(defaultValue)));
        }

    }

}
