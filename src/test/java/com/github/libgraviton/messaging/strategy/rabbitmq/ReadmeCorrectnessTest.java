package com.github.libgraviton.messaging.strategy.rabbitmq;

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
    
    private RabbitMqConnection.Builder builder;
    
    @Before
    public void setUp() {
        properties = mock(Properties.class);
        builder = spy(new RabbitMqConnection.Builder());
        doCallRealMethod().when(properties).getProperty(anyString(), anyString());
    }
    
    @Test
    public void testPropertiesBuilderMethodListComplete() {
        builder.applyProperties(properties, "context");
        builder.build();
        
        verify(properties, times(15)).getProperty(matches("context\\..*$")); // verify list is complete
        verify(properties).getProperty("context.host");
        verify(properties).getProperty("context.port");
        verify(properties).getProperty("context.user");
        verify(properties).getProperty("context.password");
        verify(properties).getProperty("context.queue.name");
        verify(properties).getProperty("context.connection.attempts");
        verify(properties).getProperty("context.connection.attempts.wait");
        verify(properties).getProperty("context.queue.durable");
        verify(properties).getProperty("context.queue.exclusive");
        verify(properties).getProperty("context.queue.autodelete");
        verify(properties).getProperty("context.exchange.name");
        verify(properties).getProperty("context.exchange.type");
        verify(properties).getProperty("context.exchange.durable");
        verify(properties).getProperty("context.routingkey");
        verify(properties).getProperty("context.virtualhost");
    }

    @Test
    public void testDefaults() {
        builder.applyProperties(properties);
        builder.build();

        verify(properties).getProperty("host", "localhost");
        verify(properties).getProperty("port", "5672");
        verify(properties).getProperty("user", "guest");
        verify(properties).getProperty("password", "guest");
        verify(properties).getProperty("queue.name", null);
        verify(properties).getProperty("connection.attempts", "-1");
        verify(properties).getProperty("connection.attempts.wait", "1.0");
        verify(properties).getProperty("queue.durable", "true");
        verify(properties).getProperty("queue.exclusive", "false");
        verify(properties).getProperty("queue.autodelete", "false");
        verify(properties).getProperty("exchange.name", null);
        verify(properties).getProperty("exchange.type", "direct");
        verify(properties).getProperty("exchange.durable", "false");
        verify(properties).getProperty("routingkey", null);
        verify(properties).getProperty("virtualhost", "/");
    }

}
