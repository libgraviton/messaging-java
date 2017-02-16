package com.github.libgraviton.messaging.strategy.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>QueueExceptionLogger</p>
 *
 * @author List of contributors {@literal <https://github.com/libgraviton/graviton-worker-base-java/graphs/contributors>}
 * @see <a href="http://swisscom.ch">http://swisscom.ch</a>
 * @version $Id: $Id
 */
class QueueExceptionLogger extends DefaultExceptionHandler {

    static final private Logger LOG = LoggerFactory.getLogger(QueueExceptionLogger.class);

    @Override
    public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
        // Only log output. WorkerQueueManager already configures setAutomaticRecoveryEnabled on the connection factory.
        LOG.warn("Message queue connection recovery not yet successful. Retry again...");
    }
}
