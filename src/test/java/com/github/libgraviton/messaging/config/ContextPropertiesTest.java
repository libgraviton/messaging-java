package com.github.libgraviton.messaging.config;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ContextPropertiesTest {

    private Properties defaultProperties;

    private ContextProperties contextProperties;

    @Before
    public void setUp() {
        defaultProperties = mock(Properties.class);
        doCallRealMethod().when(defaultProperties).getProperty(anyString(), anyString());
        doReturn("value-1-1").when(defaultProperties).getProperty("config-1.property-1");
        doReturn("value-1-2").when(defaultProperties).getProperty("config-1.property-2");
        doReturn("value-2-1").when(defaultProperties).getProperty("config-2.property-1");
        doReturn("value-2-2").when(defaultProperties).getProperty("config-2.property-2");

        contextProperties = new ContextProperties(defaultProperties, "context.");
    }

    @Test
    public void testProppertyPrefix() {
        contextProperties = new ContextProperties(defaultProperties, "config-1.");
        assertEquals("value-1-1", contextProperties.getProperty("property-1"));
        assertEquals("value-1-1", contextProperties.getProperty("property-1", "default-value"));
        assertEquals("value-1-2", contextProperties.getProperty("property-2"));
        assertEquals("value-1-2", contextProperties.getProperty("property-2", "default-value"));

        contextProperties = new ContextProperties(defaultProperties, "config-2.");
        assertEquals("value-2-1", contextProperties.getProperty("property-1"));
        assertEquals("value-2-1", contextProperties.getProperty("property-1", "default-value"));
        assertEquals("value-2-2", contextProperties.getProperty("property-2"));
        assertEquals("value-2-2", contextProperties.getProperty("property-2", "default-value"));
    }

    @Test
    public void testPropertyDefaults() {
        assertEquals("default-value", contextProperties.getProperty("does-not-exist", "default-value"));
    }

}
