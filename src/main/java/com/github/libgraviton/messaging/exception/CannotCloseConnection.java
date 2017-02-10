/**
 * Exception to be thrown on a feedback XML processing / event status update failure.
 */

package com.github.libgraviton.messaging.exception;

/**
 * <p>CannotConnectToQueue</p>
 *
 * @author List of contributors {@literal <https://github.com/libgraviton/graviton-worker-base-java/graphs/contributors>}
 * @see <a href="http://swisscom.ch">http://swisscom.ch</a>
 * @version $Id: $Id
 */
public class CannotCloseConnection extends Exception {

    private String queueName;

    public CannotCloseConnection(String queueName, Throwable cause) {
        super(String.format("Unable to close connection to queue '%s'.", queueName), cause);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }

}
