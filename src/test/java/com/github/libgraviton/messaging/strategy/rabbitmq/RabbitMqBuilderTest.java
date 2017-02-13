package com.github.libgraviton.messaging.strategy.rabbitmq;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;

public class RabbitMqBuilderTest {

    private RabbitMqConnection.Builder builder;

    private Properties properties;

    @Before
    public void setUp() {
        builder = new RabbitMqConnection.Builder();
        properties = mock(Properties.class);
        doCallRealMethod().when(properties).getProperty(anyString(), anyString());
    }

    @Test
    public void testProperties() {
        builder = new RabbitMqConnection.Builder(properties, "context");
        verify(properties).getProperty("context.host");
    }

    @Test
    public void testPrefixedProperties() {
        builder = new RabbitMqConnection.Builder(properties, "config");
        verify(properties, times(12)).getProperty(matches("^config\\..*$"));
    }

}
