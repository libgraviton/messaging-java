package com.github.libgraviton.messaging.config;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class PropertyUtilTest {

    Properties properties;

    @Before
    public void setUp() {
        properties = spy(new Properties());
    }

    @Test
    public void testBooleanProperty() {
        properties.setProperty("boolean-true", "true");
        properties.setProperty("boolean-false", "false");

        assertTrue(PropertyUtil.getBoolean(properties, "boolean-true", false));
        assertTrue(PropertyUtil.getBoolean(properties, "boolean-true-inexistent", true));
        assertFalse(PropertyUtil.getBoolean(properties, "boolean-false", true));
        assertFalse(PropertyUtil.getBoolean(properties, "boolean-false-inexistent", false));
    }

    @Test
    public void testIntProperty() {
        properties.setProperty("int-0", "0");
        properties.setProperty("int-7", "7");

        assertEquals(0, PropertyUtil.getIntger(properties, "int-0", 1));
        assertEquals(0, PropertyUtil.getIntger(properties, "int-0-inexistent", 0));
        assertEquals(7, PropertyUtil.getIntger(properties, "int-7", 1));
        assertEquals(7, PropertyUtil.getIntger(properties, "int-7-inexistent", 7));
    }

    @Test
    public void testDoubleProperty() {
        properties.setProperty("double-0", "0.0");
        properties.setProperty("double-7", "7");

        assertEquals(0.0, PropertyUtil.getDouble(properties, "double-0", 1), 0);
        assertEquals(0.0, PropertyUtil.getDouble(properties, "double-0-inexistent", 0), 0);
        assertEquals(7.0, PropertyUtil.getDouble(properties, "double-7", 1), 0);
        assertEquals(7.0, PropertyUtil.getDouble(properties, "double-7-inexistent", 7), 0);
    }

}
