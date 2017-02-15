package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.exception.CannotBuildConnection;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * Ensures that the examples in the RabbitMQ README are valid.
 *
 * IF YOU NEED TO CHANGE ANYTHING HERE, YOU NEED TO UPDATE
 * /src/main/java/com/github/libgraviton/messaging/strategy/rabbitmq/README.md ACCORDINGLY.
 */
public class ReadmeCorrectnessTest {

    private Properties properties;
    
    private JmsConnection.Builder builder;
    
    @Before
    public void setUp() {
        properties = mock(Properties.class);
        builder = spy(new JmsConnection.Builder());
        doCallRealMethod().when(properties).getProperty(anyString(), anyString());
    }
    
    @Test
    public void testPropertiesBuilderMethodListComplete() throws CannotBuildConnection {
        // ensure test fails if connectionFactory gets removed
        builder.connectionFactory(null).applyProperties(properties, "context");
        builder.build();

        // 7 + 1 default port invocation
        verify(properties, times(8)).getProperty(matches("context\\..*$")); // verify list is complete
        verify(properties).getProperty("context.host");
        verify(properties).getProperty("context.port");
        verify(properties).getProperty("context.user");
        verify(properties).getProperty("context.password");
        verify(properties).getProperty("context.queue.name");
        verify(properties).getProperty("context.connection.attempts");
        verify(properties).getProperty("context.connection.attempts.wait");
    }

    @Test
    public void testDefaults() throws CannotBuildConnection {
        builder.applyProperties(properties);
        builder.build();

        verify(properties).getProperty("host", "localhost");
        verify(properties).getProperty("port", "61616");
        verify(properties).getProperty("user", "anonymous");
        verify(properties).getProperty("password", null);
        verify(properties).getProperty("queue.name", null);
        verify(properties).getProperty("connection.attempts", "-1");
        verify(properties).getProperty("connection.attempts.wait", "1.0");
    }

}
