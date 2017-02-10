package com.github.libgraviton.messaging;

import com.github.libgraviton.messaging.exception.CannotCloseConnection;
import com.github.libgraviton.messaging.exception.CannotConnectToQueue;
import com.github.libgraviton.messaging.exception.CannotPublishMessage;
import com.github.libgraviton.messaging.exception.CannotRegisterConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a connection to a queue of any queue system.
 */
abstract public class QueueConnection {

    protected static final Logger LOG = LoggerFactory.getLogger(QueueConnection.class);

    protected String queueName;

    private int connectionAttempts = 0;

    private double connectionAttemptSleep = 1;

    private Consumer consumer;

    /**
     * Creates a connection to a queue.
     *
     * @param queueName The name of the queue
     */
    public QueueConnection(String queueName) {
        this.queueName = queueName;
    }

    /**
     * Sets the amount of connection attempts in order to connect to the queue. Set to 0, if it should try connecting
     * endlessly.
     *
     * @param connectionAttempts The amount of connection attempts
     */
    public void setConnectionAttempts(int connectionAttempts) {
        this.connectionAttempts = connectionAttempts;
    }

    /**
     * Sets the amount of seconds to wait between each connection attempt. If you want to wait less than 1 second, you
     * can just pass the value as a decimal (exception.g. 0.5 for half a second).
     *
     * @param connectionAttemptSleep The amount of seconds to wati.
     */
    public void setConnectionAttemptSleep(double connectionAttemptSleep) {
        this.connectionAttemptSleep = connectionAttemptSleep;
    }

    /**
     * Opens the connection. If the connection cannot be establishes, it waits for {@link #connectionAttemptSleep}
     * seconds and then tries again until {@link #connectionAttempts} is reached.
     *
     * @see #setConnectionAttempts(int)
     * @see #setConnectionAttemptSleep(double)
     *
     * @throws CannotConnectToQueue If connecting to the queue failed.
     */
    public void open() throws CannotConnectToQueue {
        int connectionAttempts = this.connectionAttempts;
        boolean retryEndless = connectionAttempts == 0;
        LOG.info(String.format("Connecting to queue '%s'...", queueName));
        while (connectionAttempts > 0 || retryEndless) {
            try {
                openConnection();
                break;
            } catch (CannotConnectToQueue e) {
                LOG.error(String.format("Unable to open to queue '%s': '%s'", queueName, e.getMessage()));
                // Last try failed
                if (1 == connectionAttempts) {
                    throw e;
                }
            }
            LOG.warn(String.format(
                    "Connection to queue '%s' failed. Retrying in '%s' seconds.",
                    queueName,
                    connectionAttemptSleep
            ));
            try {
                Thread.sleep((long) (connectionAttemptSleep * 1000));
            } catch (InterruptedException e) {
                LOG.warn(String.format("Thread sleep interrupted: %s", e.getMessage()));
            }
            if (!retryEndless) {
                connectionAttempts--; // avoid integer range overflow in endless mode
            }
        }
        LOG.info(String.format("Connection to queue '%s' successfully established.", queueName));
    }

    /**
     * Closes the connection
     */
    public void close() {
        LOG.info(String.format("Closing connection to queue '%s'...", queueName));
        consumer = null;
        try {
            closeConnection();
            LOG.info(String.format("Connection to queue '%s' successfully closed.", queueName));
        } catch (CannotCloseConnection e) {
            LOG.warn(String.format("Cannot successfully close queue '%s': '%s'", queueName, e.getCause().getMessage()));
        }
    }

    /**
     * Registers a consumer on the queue. Please not that you can only register one consumer per queue. This is intended
     * per design to make queue abstraction easier. If you need multiple consumers on the same queue for some reason,
     * please use a composite consumer.
     *
     * @param consumer The consumer
     *
     * @throws CannotRegisterConsumer If the consumer cannot be registered for some reason.
     */
    public void consume(Consumer consumer) throws CannotRegisterConsumer {
        LOG.info(String.format("Registering consumer on queue '%s'...", queueName));
        // This allows easier consumer recovery on queue exceptions
        if (null != this.consumer) {
            throw new CannotRegisterConsumer(
                    consumer,
                    "Another consumer is already registered. " +
                            "Please register a composite consumer If you want to use multiple consumers."
            );
        }
        try {
            openIfClosed();
        } catch (CannotConnectToQueue e) {
            throw new CannotRegisterConsumer(consumer, e);
        }
        registerConsumer(consumer);
        this.consumer = consumer;
        LOG.info(String.format("Consumer successfully registered on queue '%s'. Waiting for messages...", queueName));
    }

    /**
     * Publishes a message on the queue. If the queue has not yet been opened, it will be opened, the message published
     * and then closed again. If the queue has already been opened, it won't be closed after publishing the message.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published for some reason.
     */
    public void publish(String message) throws CannotPublishMessage {
        LOG.info(String.format("Publishing message on queue '%s': '%s", queueName, message));
        boolean wasClosed = false;
        try {
            wasClosed = openIfClosed();
            publishMessage(message);
        } catch (CannotConnectToQueue e) {
            throw new CannotPublishMessage(message, e);
        } finally {
            if (wasClosed) {
                close();
            }
        }
        LOG.info(String.format("Message successfully published on queue '%s'.", queueName));
    }

    /**
     * Opens the connection if it's currently closed.
     *
     * @return true if it was closed, otherwise false.
     *
     * @throws CannotConnectToQueue If the connection to the queue cannot be established.
     */
    public boolean openIfClosed() throws CannotConnectToQueue {
        if (!isOpen()) {
            open();
            LOG.info(String.format("Connection to queue '%s' has already been opened. Skipping...", queueName));
            return true;
        }
        return false;
    }

    /**
     * Gets the queue's name
     *
     * @return The queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Checks whether the queue is open or not.
     *
     * @return true if the queue has been opened, false if not.
     */
    abstract public boolean isOpen();

    /**
     * Does the queue system specific logic to establish a connection to the queue.
     *
     * @throws CannotConnectToQueue If the connection cannot be established.
     */
    abstract protected void openConnection() throws CannotConnectToQueue;

    /**
     * Does the queue system specific logic to register a consumer / listener.
     *
     * @param consumer The consumer to register. You most likely need to wrap this by a queue system specific consumer.
     *
     * @throws CannotRegisterConsumer
     */
    abstract protected void registerConsumer(Consumer consumer) throws CannotRegisterConsumer;

    /**
     * Does the queue system specific logic to publish a message on the queue.
     *
     * @param message The message to publish
     *
     * @throws CannotPublishMessage If the message cannot be published.
     */
    abstract protected void publishMessage(String message) throws CannotPublishMessage;

    /**
     * Does the queue specific logic to close the connection.
     *
     * @throws CannotCloseConnection If the connection cannot be closed.
     */
    abstract protected void closeConnection() throws CannotCloseConnection;

}
