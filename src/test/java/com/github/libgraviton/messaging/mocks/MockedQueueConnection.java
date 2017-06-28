package com.github.libgraviton.messaging.mocks;

import com.github.libgraviton.messaging.QueueConnection;
import com.github.libgraviton.messaging.consumer.Consumer;
import com.github.libgraviton.messaging.exception.*;

public class MockedQueueConnection extends QueueConnection {

    private MockedQueueConnection(QueueConnection.Builder builder) {
        super(builder);
    }

    @Override
    public String getConnectionName() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    protected void openConnection() throws CannotConnectToQueue {

    }

    @Override
    protected void registerConsumer(Consumer consumer) throws CannotRegisterConsumer {

    }

    @Override
    protected void publishMessage(String message) throws CannotPublishMessage {

    }

    @Override
    protected void publishMessage(byte[] message) throws CannotPublishMessage {

    }

    @Override
    protected void closeConnection() throws CannotCloseConnection {

    }

    public static class Builder extends QueueConnection.Builder<Builder> {

        @Override
        public MockedQueueConnection build() throws CannotBuildConnection {
            return new MockedQueueConnection(this);
        }

    }
}
