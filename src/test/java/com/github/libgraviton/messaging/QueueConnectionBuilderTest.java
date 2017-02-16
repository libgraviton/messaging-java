package com.github.libgraviton.messaging;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;

public class QueueConnectionBuilderTest {

    private QueueConnection.Builder builder;

    private Properties properties;

    @Before
    public void setUp() {
        builder = mock(QueueConnection.Builder.class, CALLS_REAL_METHODS);
        properties = mock(Properties.class);
        doCallRealMethod().when(properties).getProperty(anyString(), anyString());
    }

    @Test
    public void testProperties() {
        builder.applyProperties(properties);
        verify(properties).getProperty("host");
    }

    @Test
    public void testPrefixedProperties() {
        builder.applyProperties(properties, "context.");
        verify(properties, times(7)).getProperty(matches("^context\\..*$"));
    }

}
